package com.dozycoffee.wms.outbound.adapter.`in`.web.request

import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class RegisterOutboundRequest(
    @field:NotNull val warehouseId: Long?,
    @field:NotEmpty @field:Valid val items: List<RegisterOutboundItemRequest> = emptyList()
) {
    fun toCommand(): RegisterOutboundCommand {
        return RegisterOutboundCommand(requireNotNull(warehouseId), items.map { it.toCommand() })
    }
}
