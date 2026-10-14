package com.dozycoffee.wms.location_stock.application.port.`in`.result

/** 점유한 장부 행과 점유한 수량 */
data class HeldLocationStockResult(
    val locationStockId: Long,
    val locationId: Long,
    val quantity: Int
)
