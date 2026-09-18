package com.dozycoffee.wms.stock_audit.application.port.`in`

import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditItemResult
import kotlinx.coroutines.flow.Flow

interface GetStockAuditItemUseCase {
    fun getAllByStockAudit(stockAuditId: Long): Flow<StockAuditItemResult>
}
