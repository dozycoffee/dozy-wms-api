package com.dozycoffee.wms.inventory.application.port.`in`.result

import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

data class ZoneInventorySummaryResult(
    val zoneId: Long,
    val zoneCode: ZoneCode,
    val maxCapacity: Int,
    val usedCapacity: Int,
    val quantityByQualityStatus: Map<QualityStatus, Int>
) {
    val usageRate: Double
        get() = if (maxCapacity == 0) 0.0 else usedCapacity.toDouble() / maxCapacity
}
