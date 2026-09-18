package com.dozycoffee.wms.stock_audit.application.port.`in`

import com.dozycoffee.wms.stock_audit.application.port.`in`.command.CountStockAuditItemCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditItemResult

interface CountStockAuditItemUseCase {
    suspend fun count(command: CountStockAuditItemCommand): StockAuditItemResult
}
