package com.dozycoffee.wms.stock_audit.application.port.`in`

import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult

interface CloseStockAuditUseCase {
    suspend fun close(stockAuditId: Long, approvedBy: String?): StockAuditResult
}
