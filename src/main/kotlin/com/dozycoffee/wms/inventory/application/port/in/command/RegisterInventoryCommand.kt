package com.dozycoffee.wms.inventory.application.port.`in`.command

import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType

data class RegisterInventoryCommand(
    val lotId: Long,
    val locationId: Long,
    val quantity: Int,
    val referenceId: Long,
    val historyType: InventoryHistoryType
)
