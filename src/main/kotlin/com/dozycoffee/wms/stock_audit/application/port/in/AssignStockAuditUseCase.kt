package com.dozycoffee.wms.stock_audit.application.port.`in`

import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult

interface AssignStockAuditUseCase {
    suspend fun assign(stockAuditId: Long, assignee: String): StockAuditResult
}
