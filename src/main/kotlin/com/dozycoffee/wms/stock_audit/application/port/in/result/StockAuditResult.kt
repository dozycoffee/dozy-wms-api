package com.dozycoffee.wms.stock_audit.application.port.`in`.result

import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit

data class StockAuditResult(
    val stockAuditId: Long,
    val warehouseId: Long,
    val zoneId: Long,
    val status: StockAuditStatus,
    val assignee: String?,
    val approvedBy: String?
) {
    companion object {
        fun from(stockAudit: StockAudit): StockAuditResult {
            return StockAuditResult(
                requireNotNull(stockAudit.stockAuditId),
                stockAudit.warehouseId,
                stockAudit.zoneId,
                stockAudit.status,
                stockAudit.assignee,
                stockAudit.approvedBy
            )
        }
    }
}
