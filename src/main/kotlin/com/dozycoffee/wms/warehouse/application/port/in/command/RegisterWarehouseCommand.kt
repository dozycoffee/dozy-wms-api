package com.dozycoffee.wms.warehouse.application.port.`in`.command

import java.math.BigDecimal

data class RegisterWarehouseCommand(
    val warehouseName: String,
    val address: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal
)
