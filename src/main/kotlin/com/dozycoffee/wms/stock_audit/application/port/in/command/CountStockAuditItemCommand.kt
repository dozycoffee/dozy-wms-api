package com.dozycoffee.wms.stock_audit.application.port.`in`.command

data class CountStockAuditItemCommand(
    val stockAuditItemId: Long,
    val countedQuantity: Int
)
