package com.dozycoffee.wms.inventory.adapter.`in`.web.response

import com.dozycoffee.wms.inventory.application.port.`in`.result.ZoneInventorySummaryResult
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

data class ZoneInventorySummaryResponse(
    val zoneId: Long,
    val zoneCode: ZoneCode,
    val warehouseId: Long,
    val maxCapacity: Int,
    val usedCapacity: Int,
    val usageRate: Double,
    val quantityByQualityStatus: Map<QualityStatus, Int>
) {
    companion object {
        fun from(result: ZoneInventorySummaryResult): ZoneInventorySummaryResponse {
            return ZoneInventorySummaryResponse(
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
