package com.dozycoffee.wms.stock_audit.application.port.`in`

import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import kotlinx.coroutines.flow.Flow

interface GetStockAuditUseCase {
    suspend fun getById(stockAuditId: Long): StockAuditResult
    fun getAll(warehouseId: Long?, status: StockAuditStatus?): Flow<StockAuditResult>
}
