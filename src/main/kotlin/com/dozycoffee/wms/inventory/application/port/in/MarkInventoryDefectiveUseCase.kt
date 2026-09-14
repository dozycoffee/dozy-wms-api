package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult

interface MarkInventoryDefectiveUseCase {
    suspend fun markDefective(inventoryId: Long): InventoryResult
}
