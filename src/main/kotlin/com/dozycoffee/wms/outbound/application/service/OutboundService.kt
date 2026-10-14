package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.inventory.application.port.`in`.FulfillAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.outbound.application.port.`in`.CompleteOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundInspectingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundPickingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OutboundService(
    private val warehouseAccessGuard: WarehouseAccessGuard,
    private val outboundRepository: OutboundRepository,
    private val outboundItemRepository: OutboundItemRepository,
    private val outboundAllocationRepository: OutboundAllocationRepository,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val outboundAllocationHolder: OutboundAllocationHolder,
    private val getAllocationUseCase: GetAllocationUseCase,
    private val fulfillAllocationUseCase: FulfillAllocationUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val releaseLocationUseCase: ReleaseLocationUseCase,
    private val outboundFulfillmentCallRecorder: OutboundFulfillmentCallRecorder
) : StartOutboundPickingUseCase,
    StartOutboundInspectingUseCase,
    CompleteOutboundUseCase,
    GetOutboundUseCase {

    /**
     * 확정된 예약이 할당한 Lot만큼 정상(NORMAL) 재고를 점유(Allocation.HELD)한다. 재고가 부족하면 확보 가능한 만큼만 피킹한다 —
     * 부족분은 OutboundItem.shortageQuantity로 드러나고 출고 완료 때 inventory가 남은 예약을 해제한다.
     */
    @Transactional
    override suspend fun startPicking(outboundId: Long): OutboundResult {
        val outbound = findAccessibleOutboundOrThrow(outboundId)
        val items = outboundItemRepository.findAllByOutboundId(outboundId).toList()
        val allocationsByItemId: Map<Long, List<OutboundAllocation>> = outboundAllocationRepository
            .findAllByOutboundItemIds(items.mapNotNull { it.outboundItemId })
            .groupBy { it.outboundItemId }

        var totalPickedQuantity = 0
        for (item in items) {
            var pickedQuantity = 0
            for (allocation in allocationsByItemId[item.outboundItemId].orEmpty()) {
                val held = outboundAllocationHolder.hold(item, allocation, outbound.warehouseId)
                allocation.pick(held)
                outboundAllocationRepository.save(allocation)
                pickedQuantity += held
            }
            item.pick(pickedQuantity)
            outboundItemRepository.save(item)
            totalPickedQuantity += pickedQuantity
        }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(outbound.warehouseId, AreaCode.OUTBOUND)
        occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workArea.workAreaId, totalPickedQuantity))

        outbound.startPicking()
        return OutboundResult.from(outboundRepository.save(outbound))
    }

    @Transactional
    override suspend fun startInspecting(outboundId: Long): OutboundResult {
        val outbound = findAccessibleOutboundOrThrow(outboundId)
        outbound.startInspecting()
        return OutboundResult.from(outboundRepository.save(outbound))
    }

    /**
     * 검수를 마친 상품의 점유를 이행(fulfill) 확정하고, 물리적으로 반출된 만큼 Location/출고장 점유를 해제한다.
     * 예약 기반 출고는 inventory 출고 확정 호출을 같은 트랜잭션에서 아웃박스에 기록한다.
     */
    @Transactional
    override suspend fun complete(outboundId: Long): OutboundResult {
        val outbound = findAccessibleOutboundOrThrow(outboundId)
        val items = outboundItemRepository.findAllByOutboundId(outboundId).toList()

        var totalPickedQuantity = 0
        for (item in items) {
            val allocations = getAllocationUseCase
                .getAllHeldByReference(AllocationReferenceType.OUTBOUND, requireNotNull(item.outboundItemId))
                .toList()
            for (allocation in allocations) {
                val inventory = getInventoryUseCase.getById(allocation.inventoryId)
                fulfillAllocationUseCase.fulfill(allocation.allocationId)
                releaseLocationUseCase.release(ReleaseLocationCommand(inventory.locationId, allocation.quantity))
            }
            totalPickedQuantity += item.pickedQuantity ?: 0
        }

        val allocations = outboundAllocationRepository.findAllByOutboundItemIds(items.mapNotNull { it.outboundItemId })
        outboundFulfillmentCallRecorder.record(outbound, allocations)

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(outbound.warehouseId, AreaCode.OUTBOUND)
        releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, totalPickedQuantity))

        outbound.complete()
        return OutboundResult.from(outboundRepository.save(outbound))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(outboundId: Long): OutboundResult {
        return OutboundResult.from(findAccessibleOutboundOrThrow(outboundId))
    }

    @Transactional(readOnly = true)
    override fun getAll(status: OutboundStatus?): Flow<OutboundResult> {
        return warehouseAccessGuard.scoped { warehouseIds -> outboundRepository.findAll(status, warehouseIds) }
            .map { OutboundResult.from(it) }
    }

    private suspend fun findAccessibleOutboundOrThrow(outboundId: Long): Outbound {
        val outbound = outboundRepository.findById(outboundId) ?: throw OutboundNotFoundException()
        warehouseAccessGuard.require(outbound.warehouseId)
        return outbound
    }
}
