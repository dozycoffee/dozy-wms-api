package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inventory.application.port.out.InventoryHistoryRepository
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class InventoryHistoryPersistenceAdapter(
    private val inventoryHistoryR2dbcRepository: InventoryHistoryR2dbcRepository
) : InventoryHistoryRepository {

    override suspend fun save(inventoryHistory: InventoryHistory): InventoryHistory {
        val entity = InventoryHistoryEntity.from(inventoryHistory)
        return inventoryHistoryR2dbcRepository.save(entity).toDomain()
    }

    override fun findAll(
        inventoryId: Long?,
        historyType: InventoryHistoryType?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): Flow<InventoryHistory> {
        val historyTypeCode = historyType?.let { CommonCodes.toCode(HISTORY_TYPE_GROUP, it) }
        return inventoryHistoryR2dbcRepository.findAllFiltered(inventoryId, historyTypeCode, from, to)
            .map { it.toDomain() }
    }

    override fun findRecentByInventoryId(inventoryId: Long, limit: Int): Flow<InventoryHistory> {
        return inventoryHistoryR2dbcRepository.findRecentByInventoryId(inventoryId, limit).map { it.toDomain() }
    }

    companion object {
        private const val HISTORY_TYPE_GROUP = "INVENTORY_HISTORY_TYPE"
    }
}
