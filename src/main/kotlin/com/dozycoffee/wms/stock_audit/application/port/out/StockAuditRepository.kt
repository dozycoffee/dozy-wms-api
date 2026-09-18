package com.dozycoffee.wms.stock_audit.application.port.out

import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import kotlinx.coroutines.flow.Flow

interface StockAuditRepository {
    suspend fun save(stockAudit: StockAudit): StockAudit
    suspend fun findById(stockAuditId: Long): StockAudit?
    fun findAll(warehouseId: Long?, status: StockAuditStatus?): Flow<StockAudit>
}
