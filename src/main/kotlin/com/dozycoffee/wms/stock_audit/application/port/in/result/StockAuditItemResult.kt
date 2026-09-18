package com.dozycoffee.wms.stock_audit.application.port.`in`.result

import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem

data class StockAuditItemResult(
    val stockAuditItemId: Long,
    val stockAuditId: Long,
    val inventoryId: Long,
    val snapshotQuantity: Int,
    val countedQuantity: Int?,
    val discrepancy: Int?,
    val hasUncommittedMovement: Boolean
) {
    companion object {
        fun from(stockAuditItem: StockAuditItem): StockAuditItemResult {
            return StockAuditItemResult(
                requireNotNull(stockAuditItem.stockAuditItemId),
                stockAuditItem.stockAuditId,
                stockAuditItem.inventoryId,
                stockAuditItem.snapshotQuantity,
                stockAuditItem.countedQuantity,
                stockAuditItem.discrepancy,
                stockAuditItem.hasUncommittedMovement
            )
        }
    }
}
