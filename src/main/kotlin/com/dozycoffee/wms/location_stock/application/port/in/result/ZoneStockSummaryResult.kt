package com.dozycoffee.wms.location_stock.application.port.`in`.result

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

data class ZoneStockSummaryResult(
    val zoneId: Long,
    val zoneCode: ZoneCode,
    val warehouseId: Long,
    val maxCapacity: Int,
    val usedCapacity: Int,
    val quantityByQualityStatus: Map<StockQualityStatus, Int>
) {
    val usageRate: Double
        get() = if (maxCapacity == 0) 0.0 else usedCapacity.toDouble() / maxCapacity
}
