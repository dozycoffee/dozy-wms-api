package com.dozycoffee.wms.outbound.application.port.`in`.command

import java.time.LocalDate

/** 확정된 예약(RESERVATION_CONFIRMED)의 내용. 상품별로 Lot 단위 할당을 담는다 */
data class CreateOutboundFromReservationCommand(
    val warehouseId: Long,
    val reservationId: Long,
    val channel: String?,
    val externalOrderId: String?,
    val items: List<Item>
) {
    data class Item(val productId: Long, val allocations: List<Allocation>)

    data class Allocation(val inventoryId: Long, val lotNumber: String, val expirationDate: LocalDate?, val quantity: Int)
}
