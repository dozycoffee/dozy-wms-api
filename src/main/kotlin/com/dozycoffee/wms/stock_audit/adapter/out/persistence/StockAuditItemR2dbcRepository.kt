package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface StockAuditItemR2dbcRepository : CoroutineCrudRepository<StockAuditItemEntity, Long> {

    @Query("SELECT * FROM stock_audit_item WHERE stock_audit_id = :stockAuditId")
    fun findAllByStockAuditId(stockAuditId: Long): Flow<StockAuditItemEntity>
}
