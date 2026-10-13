package com.dozycoffee.wms.warehouse_member.application.port.`in`.result

data class PublishWarehouseAccessEventsResult(
    val published: Int,
    val failed: Int
) {
    val processed: Int get() = published + failed
}
