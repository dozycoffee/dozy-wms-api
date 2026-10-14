package com.dozycoffee.wms.location_stock.adapter.`in`.web.response

import com.dozycoffee.wms.location_stock.application.port.`in`.result.ZoneStockSummaryResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

data class ZoneStockSummaryResponse(
    val zoneId: Long,
    val zoneCode: ZoneCode,
    val warehouseId: Long,
    val maxCapacity: Int,
    val usedCapacity: Int,
    val usageRate: Double,
    val quantityByQualityStatus: Map<StockQualityStatus, Int>
) {
    companion object {
        fun from(result: ZoneStockSummaryResult): ZoneStockSummaryResponse {
            return ZoneStockSummaryResponse(
                result.zoneId,
                result.zoneCode,
                result.warehouseId,
                result.maxCapacity,
                result.usedCapacity,
                result.usageRate,
                result.quantityByQualityStatus
            )
        }
    }
}
