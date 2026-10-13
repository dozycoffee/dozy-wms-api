package com.dozycoffee.wms.inventory_call.application.port.`in`.command

import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType

data class RecordInventoryCallCommand(
    val callType: InventoryCallType,
    val warehouseId: Long,
    val idempotencyKey: String,
    val requestPath: String,
    val requestBody: String
)
