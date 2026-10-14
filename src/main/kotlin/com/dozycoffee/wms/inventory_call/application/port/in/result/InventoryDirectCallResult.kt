package com.dozycoffee.wms.inventory_call.application.port.`in`.result

/** inventory 직접 호출의 결과. 받아들여짐 / 업무상 거부(다시 보내도 같음) / 일시적으로 보낼 수 없음 */
sealed interface InventoryDirectCallResult {
    data class Accepted(val responseBody: String?) : InventoryDirectCallResult

    data class Rejected(val status: Int?, val responseBody: String?) : InventoryDirectCallResult

    data class Unavailable(val reason: String) : InventoryDirectCallResult
}
