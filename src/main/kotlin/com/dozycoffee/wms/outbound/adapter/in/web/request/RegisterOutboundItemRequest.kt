package com.dozycoffee.wms.outbound.adapter.`in`.web.request

import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundItemCommand
import jakarta.validation.constraints.NotNull

data class RegisterOutboundItemRequest(
    @field:NotNull val productId: Long?,
    @field:NotNull val requestedQuantity: Int?
) {
    fun toCommand(): RegisterOutboundItemCommand {
        return RegisterOutboundItemCommand(requireNotNull(productId), requireNotNull(requestedQuantity))
    }
}
