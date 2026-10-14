package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.outbound.application.port.`in`.ApproveOutboundCancelUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.RequestOutboundCancelUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotCancelableException
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 출고 취소(시나리오 결정 3). 취소가 확정될 때만 inventory 예약 해제를 같은 트랜잭션에서 아웃박스에 기록한다 */
@Service
class OutboundCancelService(
    private val warehouseAccessGuard: WarehouseAccessGuard,
    private val outboundRepository: OutboundRepository,
    private val outboundItemRepository: OutboundItemRepository,
    private val outboundAllocationRepository: OutboundAllocationRepository,
    private val outboundAllocationHolder: OutboundAllocationHolder,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val outboundReleaseCallRecorder: OutboundReleaseCallRecorder
) : RequestOutboundCancelUseCase, ApproveOutboundCancelUseCase {

    @Transactional
    override suspend fun requestCancel(outboundId: Long): OutboundResult {
        val outbound = findAccessibleOutboundOrThrow(outboundId)
        when (outbound.status) {
            OutboundStatus.CANCEL_REQUESTED, OutboundStatus.CANCELED -> return OutboundResult.from(outbound)
            OutboundStatus.COMPLETED -> throw OutboundNotCancelableException()
            OutboundStatus.REQUESTED -> {
                outbound.cancelBeforePicking()
                outboundReleaseCallRecorder.record(outbound)
            }
            OutboundStatus.PICKING, OutboundStatus.INSPECTING -> outbound.requestCancel()
        }
        return OutboundResult.from(outboundRepository.save(outbound))
    }

    @Transactional
    override suspend fun approveCancel(outboundId: Long): OutboundResult {
        val outbound = findAccessibleOutboundOrThrow(outboundId)
        if (outbound.status == OutboundStatus.CANCELED) return OutboundResult.from(outbound)
        outbound.approveCancel()

        val items = outboundItemRepository.findAllByOutboundId(outboundId).toList()
        outboundAllocationRepository.findAllByOutboundItemIds(items.mapNotNull { it.outboundItemId })
            .forEach { outboundAllocationHolder.release(it) }
        val totalPickedQuantity = items.sumOf { it.pickedQuantity ?: 0 }
        if (totalPickedQuantity > 0) {
            val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(outbound.warehouseId, AreaCode.OUTBOUND)
            releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, totalPickedQuantity))
        }
        outboundReleaseCallRecorder.record(outbound)
        return OutboundResult.from(outboundRepository.save(outbound))
    }

    private suspend fun findAccessibleOutboundOrThrow(outboundId: Long): Outbound {
        val outbound = outboundRepository.findById(outboundId) ?: throw OutboundNotFoundException()
        warehouseAccessGuard.require(outbound.warehouseId)
        return outbound
    }
}
