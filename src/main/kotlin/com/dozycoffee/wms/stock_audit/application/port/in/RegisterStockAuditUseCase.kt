package com.dozycoffee.wms.stock_audit.application.port.`in`

import com.dozycoffee.wms.stock_audit.application.port.`in`.command.RegisterStockAuditCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult

interface RegisterStockAuditUseCase {
    suspend fun register(command: RegisterStockAuditCommand): StockAuditResult
}
