package com.dozycoffee.wms.stock_audit.adapter.`in`.web.request

import com.dozycoffee.wms.stock_audit.application.port.`in`.command.RegisterStockAuditCommand
import jakarta.validation.constraints.NotNull

data class RegisterStockAuditRequest(
    @field:NotNull val warehouseId: Long?,
    @field:NotNull val zoneId: Long?
) {
    fun toCommand(): RegisterStockAuditCommand {
        return RegisterStockAuditCommand(requireNotNull(warehouseId), requireNotNull(zoneId))
    }
}
