package com.dozycoffee.wms.stock_audit.application.port.out

import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import kotlinx.coroutines.flow.Flow

interface StockAuditItemRepository {
    suspend fun save(stockAuditItem: StockAuditItem): StockAuditItem
    suspend fun findById(stockAuditItemId: Long): StockAuditItem?
    fun findAllByStockAuditId(stockAuditId: Long): Flow<StockAuditItem>
}
