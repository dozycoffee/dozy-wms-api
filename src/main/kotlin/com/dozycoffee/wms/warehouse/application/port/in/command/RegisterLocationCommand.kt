package com.dozycoffee.wms.warehouse.application.port.`in`.command

data class RegisterLocationCommand(
    val zoneId: Long,
    val locationCode: String,
    val maxCapacity: Int
)
