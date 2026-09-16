package com.dozycoffee.wms.inventory.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface InventoryR2dbcRepository : CoroutineCrudRepository<InventoryEntity, Long> {

    @Query("SELECT * FROM inventory WHERE inventory_id = :inventoryId AND deleted_at IS NULL")
    suspend fun findActiveById(inventoryId: Long): InventoryEntity?

    @Query(
        """
        SELECT * FROM inventory
        WHERE deleted_at IS NULL
          AND (:locationId IS NULL OR location_id = :locationId)
          AND (:productId IS NULL OR product_id = :productId)
          AND (:qualityStatus IS NULL OR quality_status = :qualityStatus)
        """
    )
    fun findAllActive(locationId: Long?, productId: Long?, qualityStatus: String?): Flow<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE deleted_at IS NULL AND lot_id = :lotId")
    fun findAllActiveByLotId(lotId: Long): Flow<InventoryEntity>
}
