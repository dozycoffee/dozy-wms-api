package com.dozycoffee.wms.location_stock.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface LocationStockR2dbcRepository : CoroutineCrudRepository<LocationStockEntity, Long> {

    @Query(
        """
        SELECT * FROM location_stock
        WHERE location_id = :locationId AND product_id = :productId
          AND lot_number = :lotNumber AND quality_status = :qualityStatus
        FOR UPDATE
        """
    )
    suspend fun findByKeyForUpdate(
        locationId: Long,
        productId: Long,
        lotNumber: String,
        qualityStatus: String
    ): LocationStockEntity?

    fun findAllByLocationId(locationId: Long): Flow<LocationStockEntity>

    @Query("SELECT * FROM location_stock WHERE location_stock_id = :locationStockId FOR UPDATE")
    suspend fun findByIdForUpdate(locationStockId: Long): LocationStockEntity?

    fun findAllByLocationStockIdIn(locationStockIds: Collection<Long>): Flow<LocationStockEntity>

    @Query(
        """
        SELECT ls.* FROM location_stock ls
        JOIN location l ON l.location_id = ls.location_id
        JOIN zone z ON z.zone_id = l.zone_id
        WHERE z.warehouse_id = :warehouseId AND ls.product_id = :productId
          AND ls.lot_number = :lotNumber AND ls.quality_status = :qualityStatus
          AND ls.quantity - ls.held_quantity > 0
        ORDER BY ls.quantity - ls.held_quantity DESC, l.location_code ASC
        FOR UPDATE OF ls
        """
    )
    fun findAvailableForUpdate(
        warehouseId: Long,
        productId: Long,
        lotNumber: String,
        qualityStatus: String
    ): Flow<LocationStockEntity>

    @Query("SELECT COUNT(*) FROM location_stock WHERE product_id = :productId AND lot_number = :lotNumber")
    suspend fun countByProductIdAndLotNumber(productId: Long, lotNumber: String): Long

    @Query("SELECT COUNT(*) FROM location_stock WHERE location_stock_id = :locationStockId AND updated_at > :since")
    suspend fun countUpdatedAfter(locationStockId: Long, since: LocalDateTime): Long
}
