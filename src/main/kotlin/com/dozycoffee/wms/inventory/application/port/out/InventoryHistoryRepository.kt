package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.model.InventoryHistory

interface InventoryHistoryRepository {
    suspend fun save(inventoryHistory: InventoryHistory): InventoryHistory
}
