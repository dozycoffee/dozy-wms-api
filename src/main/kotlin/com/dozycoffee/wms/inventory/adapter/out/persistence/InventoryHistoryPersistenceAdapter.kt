package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.inventory.application.port.out.InventoryHistoryRepository
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import org.springframework.stereotype.Component

@Component
class InventoryHistoryPersistenceAdapter(
    private val inventoryHistoryR2dbcRepository: InventoryHistoryR2dbcRepository
) : InventoryHistoryRepository {

    override suspend fun save(inventoryHistory: InventoryHistory): InventoryHistory {
        val entity = InventoryHistoryEntity.from(inventoryHistory)
        return inventoryHistoryR2dbcRepository.save(entity).toDomain()
    }
}
