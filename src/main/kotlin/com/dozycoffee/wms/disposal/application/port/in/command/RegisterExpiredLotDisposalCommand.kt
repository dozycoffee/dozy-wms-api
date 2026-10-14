package com.dozycoffee.wms.disposal.application.port.`in`.command

/** inventory의 Lot 경과(LOT_EXPIRED) 이벤트 하나. 창고별 폐기 예정 전환 수량을 담는다 */
data class RegisterExpiredLotDisposalCommand(
    val eventId: String,
    val productId: Long,
    val lotNumber: String,
    val transfers: List<Transfer>
) {
    data class Transfer(val warehouseId: Long, val quantity: Int)
}
