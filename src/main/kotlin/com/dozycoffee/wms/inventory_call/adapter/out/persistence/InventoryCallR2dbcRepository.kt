package com.dozycoffee.wms.inventory_call.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface InventoryCallR2dbcRepository : CoroutineCrudRepository<InventoryCallEntity, Long> {

    suspend fun findByIdempotencyKey(idempotencyKey: String): InventoryCallEntity?

    @Query("SELECT * FROM inventory_call ORDER BY inventory_call_id DESC LIMIT :limit")
    fun findRecent(limit: Int): Flow<InventoryCallEntity>

    @Query("SELECT * FROM inventory_call WHERE status = :status ORDER BY inventory_call_id DESC LIMIT :limit")
    fun findRecentByStatus(status: String, limit: Int): Flow<InventoryCallEntity>

    @Query(
        """
        SELECT c.* FROM inventory_call c
        WHERE c.status = 'PENDING'
          AND c.next_attempt_at <= :now
          AND NOT EXISTS (
              SELECT 1 FROM inventory_call p
              WHERE p.warehouse_id = c.warehouse_id
                AND p.status IN ('PENDING', 'DEAD')
                AND p.inventory_call_id < c.inventory_call_id
          )
        ORDER BY c.inventory_call_id
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """
    )
    fun findDispatchable(now: LocalDateTime, limit: Int): Flow<InventoryCallEntity>
}
