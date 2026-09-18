package com.dozycoffee.wms.inventory.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface InventoryHistoryR2dbcRepository : CoroutineCrudRepository<InventoryHistoryEntity, Long> {

    @Query(
        """
        SELECT * FROM inventory_history
        WHERE (:inventoryId IS NULL OR inventory_id = :inventoryId)
          AND (:historyType IS NULL OR history_type = :historyType)
          AND (:from IS NULL OR created_at >= :from)
          AND (:to IS NULL OR created_at < :to)
        ORDER BY created_at DESC
        """
    )
    fun findAllFiltered(
        inventoryId: Long?,
        historyType: String?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): Flow<InventoryHistoryEntity>

    @Query(
        """
        SELECT * FROM inventory_history
        WHERE inventory_id = :inventoryId
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun findRecentByInventoryId(inventoryId: Long, limit: Int): Flow<InventoryHistoryEntity>
}
