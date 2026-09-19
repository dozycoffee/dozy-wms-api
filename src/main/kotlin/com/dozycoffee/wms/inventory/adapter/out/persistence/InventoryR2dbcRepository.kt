package com.dozycoffee.wms.inventory.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface InventoryR2dbcRepository : CoroutineCrudRepository<InventoryEntity, Long> {

    @Query("SELECT * FROM inventory WHERE inventory_id = :inventoryId AND deleted_at IS NULL")
    suspend fun findActiveById(inventoryId: Long): InventoryEntity?

    @Query(
        """
        SELECT i.* FROM inventory i
        LEFT JOIN lot l ON l.lot_id = i.lot_id
        LEFT JOIN location loc ON loc.location_id = i.location_id
        LEFT JOIN zone z ON z.zone_id = loc.zone_id
        WHERE i.deleted_at IS NULL
          AND (:locationId IS NULL OR i.location_id = :locationId)
          AND (:productId IS NULL OR i.product_id = :productId)
          AND (:qualityStatus IS NULL OR i.quality_status = :qualityStatus)
          AND (:warehouseIds IS NULL OR z.warehouse_id IN (:warehouseIds))
        ORDER BY
          CASE WHEN :sortBy = 'QUANTITY' THEN i.quantity END ASC,
          CASE WHEN :sortBy = 'EXPIRATION_DATE' THEN l.expiration_date END ASC,
          CASE WHEN :sortBy = 'INBOUND_DATE' THEN i.created_at END ASC,
          i.inventory_id ASC
        """
    )
    fun findAllActive(
        locationId: Long?,
        productId: Long?,
        qualityStatus: String?,
        sortBy: String?,
        warehouseIds: List<Long>?
    ): Flow<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE deleted_at IS NULL AND lot_id = :lotId")
    fun findAllActiveByLotId(lotId: Long): Flow<InventoryEntity>
}
