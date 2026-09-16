package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult

interface ConfirmInventoryDisposalUseCase {
    suspend fun confirmDisposal(inventoryId: Long, referenceId: Long): InventoryResult
}
