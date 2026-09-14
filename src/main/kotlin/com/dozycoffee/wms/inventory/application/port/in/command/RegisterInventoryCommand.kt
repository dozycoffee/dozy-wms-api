package com.dozycoffee.wms.inventory.application.port.`in`.command

data class RegisterInventoryCommand(
    val lotId: Long,
    val locationId: Long,
    val quantity: Int
)
