package com.dozycoffee.wms.warehouse.application.port.`in`.command

import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode

data class RegisterWorkAreaCommand(
    val warehouseId: Long,
    val areaCode: AreaCode
)
