package com.dozycoffee.wms.stock_audit.application.port.`in`

import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult

interface CompleteStockAuditUseCase {
    suspend fun complete(stockAuditId: Long): StockAuditResult
}
