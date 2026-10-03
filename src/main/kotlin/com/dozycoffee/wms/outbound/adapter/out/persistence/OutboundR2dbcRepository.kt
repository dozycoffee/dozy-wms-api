package com.dozycoffee.wms.outbound.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface OutboundR2dbcRepository : CoroutineCrudRepository<OutboundEntity, Long> {

    @Query(
        """
        SELECT * FROM outbound
        WHERE (:status IS NULL OR status = :status)
        """
    )
    fun findAllOutbounds(status: String?): Flow<OutboundEntity>

    @Query(
        """
        SELECT * FROM outbound
        WHERE (:status IS NULL OR status = :status)
          AND warehouse_id IN (:warehouseIds)
        """
    )
    fun findAllOutboundsInWarehouses(status: String?, warehouseIds: List<Long>): Flow<OutboundEntity>
}
