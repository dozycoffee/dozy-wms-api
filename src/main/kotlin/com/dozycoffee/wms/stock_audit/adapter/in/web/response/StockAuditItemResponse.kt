package com.dozycoffee.wms.stock_audit.adapter.`in`.web.response

import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditItemResult

data class StockAuditItemResponse(
    val stockAuditItemId: Long,
    val stockAuditId: Long,
    val inventoryId: Long,
    val snapshotQuantity: Int,
    val countedQuantity: Int?,
    val discrepancy: Int?,
    val hasUncommittedMovement: Boolean
) {
    companion object {
        fun from(result: StockAuditItemResult): StockAuditItemResponse {
            return StockAuditItemResponse(
                result.stockAuditItemId,
                result.stockAuditId,
                result.inventoryId,
                result.snapshotQuantity,
                result.countedQuantity,
                result.discrepancy,
                result.hasUncommittedMovement
            )
        }
    }
}
