package com.dozycoffee.wms.stock_audit.application.port.`in`.command

data class RegisterStockAuditCommand(
    val warehouseId: Long,
    val zoneId: Long
)
