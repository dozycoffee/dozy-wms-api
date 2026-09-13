package com.dozycoffee.wms.warehouse.application.port.`in`.command

import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

data class RegisterZoneCommand(
    val warehouseId: Long,
    val zoneCode: ZoneCode
)
