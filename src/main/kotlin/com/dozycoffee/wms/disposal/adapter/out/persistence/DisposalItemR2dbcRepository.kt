package com.dozycoffee.wms.disposal.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface DisposalItemR2dbcRepository : CoroutineCrudRepository<DisposalItemEntity, Long> {
    fun findAllByDisposalId(disposalId: Long): Flow<DisposalItemEntity>

    @Query(
        """
        SELECT COALESCE(SUM(di.quantity), 0) FROM disposal_item di
        JOIN disposal d ON d.disposal_id = di.disposal_id
        WHERE di.location_stock_id = :locationStockId AND d.status <> 'COMPLETED'
        """
    )
    suspend fun sumPendingQuantityByLocationStockId(locationStockId: Long): Long
}
