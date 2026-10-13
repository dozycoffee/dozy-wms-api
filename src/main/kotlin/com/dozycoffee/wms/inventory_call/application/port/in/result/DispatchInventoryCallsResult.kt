package com.dozycoffee.wms.inventory_call.application.port.`in`.result

data class DispatchInventoryCallsResult(
    val sent: Int,
    val retrying: Int,
    val dead: Int
) {
    val processed: Int get() = sent + retrying + dead
}
