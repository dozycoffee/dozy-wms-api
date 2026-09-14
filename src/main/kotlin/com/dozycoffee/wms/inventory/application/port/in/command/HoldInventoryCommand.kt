package com.dozycoffee.wms.inventory.application.port.`in`.command

import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType

data class HoldInventoryCommand(
    val inventoryId: Long,
    val referenceType: AllocationReferenceType,
    val referenceId: Long,
    val quantity: Int
)
