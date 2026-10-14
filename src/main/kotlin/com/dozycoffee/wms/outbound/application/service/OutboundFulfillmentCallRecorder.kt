package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/**
 * 출고 완료를 inventory 출고 확정(`POST /reservations/{id}/fulfillment`) 한 건으로 아웃박스에 기록한다 (ADR-0023).
 * inventory는 예약의 모든 할당 재고 행을 한 번씩 요구하므로 피킹하지 못한 행도 0으로 담는다.
 */
@Component
class OutboundFulfillmentCallRecorder(
    private val recordInventoryCallUseCase: RecordInventoryCallUseCase,
    private val jsonMapper: JsonMapper
) {

    suspend fun record(outbound: Outbound, allocations: List<OutboundAllocation>) {
        val reservationId: Long = outbound.reservationId ?: return
        val outboundId: Long = requireNotNull(outbound.outboundId)
        val body = FulfillmentRequestBody(
            allocations.map { FulfillmentAllocationBody(it.inventoryId, it.pickedQuantity ?: 0) }
        )
        recordInventoryCallUseCase.record(
            RecordInventoryCallCommand(
                InventoryCallType.FULFILLMENT,
                outbound.warehouseId,
                "wms-fulfillment-$outboundId",
                "/api/v1/reservations/$reservationId/fulfillment",
                jsonMapper.writeValueAsString(body)
            )
        )
    }

    private data class FulfillmentRequestBody(val allocations: List<FulfillmentAllocationBody>)

    private data class FulfillmentAllocationBody(val inventoryId: Long, val shippedQuantity: Int)
}
