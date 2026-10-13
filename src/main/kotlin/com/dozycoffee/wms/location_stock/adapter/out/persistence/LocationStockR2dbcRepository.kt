package com.dozycoffee.wms.location_stock.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

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
}
