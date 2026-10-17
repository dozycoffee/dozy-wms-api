package com.dozycoffee.wms.location_stock.application.port.`in`.result

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import java.time.LocalDateTime

data class LocationStockDetailResult(
    val locationStockId: Long,
    val warehouseId: Long,
    val zoneId: Long,
    val zoneCode: ZoneCode,
    val locationId: Long,
    val locationCode: String,
    val productId: Long,
    val lotNumber: String,
    val qualityStatus: StockQualityStatus,
    val quantity: Int,
    val heldQuantity: Int,
    val updatedAt: LocalDateTime?
) {
    val availableQuantity: Int
        get() = quantity - heldQuantity
}
