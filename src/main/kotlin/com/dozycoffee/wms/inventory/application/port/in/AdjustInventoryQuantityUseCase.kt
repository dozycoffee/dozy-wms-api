package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult

interface AdjustInventoryQuantityUseCase {
    suspend fun adjust(inventoryId: Long, newQuantity: Int, referenceId: Long): InventoryResult
}
