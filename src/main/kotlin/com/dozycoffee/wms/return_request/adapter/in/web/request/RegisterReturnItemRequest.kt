package com.dozycoffee.wms.return_request.adapter.`in`.web.request

import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnItemCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class RegisterReturnItemRequest(
    @field:NotNull val productId: Long?,
    @field:Min(1) val expectedQuantity: Int?
) {
    fun toCommand(): RegisterReturnItemCommand {
        return RegisterReturnItemCommand(
            requireNotNull(productId),
            requireNotNull(expectedQuantity)
        )
    }
}
