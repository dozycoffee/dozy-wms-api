package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface InventoryHistoryRepository {
    suspend fun save(inventoryHistory: InventoryHistory): InventoryHistory

    fun findAll(
        inventoryId: Long?,
        historyType: InventoryHistoryType?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): Flow<InventoryHistory>
}
