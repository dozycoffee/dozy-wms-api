package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface StockAuditR2dbcRepository : CoroutineCrudRepository<StockAuditEntity, Long> {

    @Query(
        """
        SELECT * FROM stock_audit
        WHERE (:warehouseId IS NULL OR warehouse_id = :warehouseId)
          AND (:status IS NULL OR status = :status)
        """
    )
    fun findAllStockAudits(warehouseId: Long?, status: String?): Flow<StockAuditEntity>
}
