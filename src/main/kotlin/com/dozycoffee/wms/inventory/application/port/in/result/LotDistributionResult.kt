package com.dozycoffee.wms.inventory.application.port.`in`.result

import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

data class LotDistributionResult(
    val locationId: Long,
    val zoneId: Long,
    val zoneCode: ZoneCode,
    val quantity: Int
)
