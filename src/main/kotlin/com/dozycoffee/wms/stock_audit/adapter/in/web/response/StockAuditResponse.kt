package com.dozycoffee.wms.stock_audit.adapter.`in`.web.response

import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus

data class StockAuditResponse(
    val stockAuditId: Long,
    val warehouseId: Long,
    val zoneId: Long,
    val status: StockAuditStatus,
    val assignee: String?,
    val approvedBy: String?
) {
    companion object {
        fun from(result: StockAuditResult): StockAuditResponse {
            return StockAuditResponse(
                result.stockAuditId,
                result.warehouseId,
                result.zoneId,
                result.status,
                result.assignee,
                result.approvedBy
            )
        }
    }
}
