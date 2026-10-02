package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface StockAuditR2dbcRepository : CoroutineCrudRepository<StockAuditEntity, Long> {

    @Query(
        """
        SELECT * FROM stock_audit
        WHERE (:status IS NULL OR status = :status)
        """
    )
    fun findAllStockAudits(status: String?): Flow<StockAuditEntity>

    @Query(
        """
        SELECT * FROM stock_audit
        WHERE (:status IS NULL OR status = :status)
          AND warehouse_id IN (:warehouseIds)
        """
    )
    fun findAllStockAuditsInWarehouses(status: String?, warehouseIds: List<Long>): Flow<StockAuditEntity>
}
