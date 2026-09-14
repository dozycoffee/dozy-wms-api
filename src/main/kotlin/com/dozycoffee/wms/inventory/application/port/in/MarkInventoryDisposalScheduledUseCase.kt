package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult

interface MarkInventoryDisposalScheduledUseCase {
    suspend fun markDisposalScheduled(inventoryId: Long): InventoryResult
}
