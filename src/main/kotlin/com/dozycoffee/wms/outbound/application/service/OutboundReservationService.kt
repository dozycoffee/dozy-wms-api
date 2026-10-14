package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.outbound.application.port.`in`.CreateOutboundFromReservationUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.command.CreateOutboundFromReservationCommand
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 예약 확정 이벤트로 출고를 만든다 (ADR-0017 결정 3). 이벤트는 최소 한 번 전달되므로 같은 예약은 한 번만 만든다 */
@Service
class OutboundReservationService(
    private val outboundRepository: OutboundRepository,
    private val outboundItemRepository: OutboundItemRepository,
    private val outboundAllocationRepository: OutboundAllocationRepository
) : CreateOutboundFromReservationUseCase {

    @Transactional
    override suspend fun create(command: CreateOutboundFromReservationCommand): Boolean {
        if (outboundRepository.findByWarehouseIdAndReservationId(command.warehouseId, command.reservationId) != null) {
            return false
        }
        val outbound = outboundRepository.save(
            Outbound.createForReservation(command.warehouseId, command.reservationId, command.channel, command.externalOrderId)
        )
        for (item in command.items) {
            val savedItem = outboundItemRepository.save(
                OutboundItem.create(outbound.outboundId, item.productId, item.allocations.sumOf { it.quantity })
            )
            for (allocation in item.allocations) {
                outboundAllocationRepository.save(
                    OutboundAllocation.create(
                        savedItem.outboundItemId,
                        allocation.inventoryId,
                        allocation.lotNumber,
                        allocation.expirationDate,
                        allocation.quantity
                    )
                )
            }
        }
        return true
    }
}
