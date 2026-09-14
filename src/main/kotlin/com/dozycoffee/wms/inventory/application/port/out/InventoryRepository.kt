package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.Inventory
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    suspend fun save(inventory: Inventory): Inventory
    suspend fun findById(inventoryId: Long): Inventory?
    fun findAll(locationId: Long?, productId: Long?, qualityStatus: QualityStatus?): Flow<Inventory>
}
