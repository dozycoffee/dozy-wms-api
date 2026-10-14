package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterExpiredLotDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterExpiredLotDisposalCommand
import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.application.port.out.DisposalRepository
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.location_stock.application.port.`in`.ScheduleLocationStockDisposalUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Lot 경과 이벤트로 폐기 신청을 만든다 (ADR-0024). 이벤트가 알려 준 수량을 가용 수량이 많은 Location부터 폐기 예정으로 옮기고
 * 옮긴 행마다 폐기 상품을 만든다. 승인과 폐기 처리장 이동은 사람이 한다. 장부의 정상 수량이 이벤트 수량보다 적으면 옮길 수 있는 만큼만
 * 옮기고 로그를 남긴다(inventory 대사가 드러낸다).
 */
@Service
class ExpiredLotDisposalService(
    private val disposalRepository: DisposalRepository,
    private val disposalItemRepository: DisposalItemRepository,
    private val scheduleLocationStockDisposalUseCase: ScheduleLocationStockDisposalUseCase
) : RegisterExpiredLotDisposalUseCase {

    @Transactional
    override suspend fun register(command: RegisterExpiredLotDisposalCommand): Int {
        var created = 0
        for (transfer in command.transfers) {
            if (disposalRepository.existsBySourceEventIdAndWarehouseId(command.eventId, transfer.warehouseId)) continue
            val moved = scheduleLocationStockDisposalUseCase.moveToDisposalScheduled(
                transfer.warehouseId, command.productId, command.lotNumber, transfer.quantity
            )
            val movedQuantity = moved.sumOf { it.quantity }
            if (movedQuantity < transfer.quantity) {
                log.warn(
                    "LOT_EXPIRED 수량보다 장부의 정상 수량이 적다: eventId={}, warehouseId={}, productId={}, lotNumber={}, 이벤트={}, 옮김={}",
                    command.eventId, transfer.warehouseId, command.productId, command.lotNumber, transfer.quantity, movedQuantity
                )
            }
            if (moved.isEmpty()) continue
            val disposal = disposalRepository.save(Disposal.create(transfer.warehouseId, command.eventId))
            moved.forEach {
                disposalItemRepository.save(DisposalItem.create(disposal.disposalId, it.locationStockId, it.quantity, DisposalReason.EXPIRED))
            }
            created++
        }
        return created
    }

    private companion object {
        val log = LoggerFactory.getLogger(ExpiredLotDisposalService::class.java)
    }
}
