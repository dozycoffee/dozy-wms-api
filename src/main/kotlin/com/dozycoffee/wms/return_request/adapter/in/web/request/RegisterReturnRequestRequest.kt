package com.dozycoffee.wms.return_request.adapter.`in`.web.request

import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnRequestCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class RegisterReturnRequestRequest(
    @field:NotNull val warehouseId: Long?,
    @field:NotEmpty @field:Valid val items: List<RegisterReturnItemRequest> = emptyList()
) {
    fun toCommand(): RegisterReturnRequestCommand {
        return RegisterReturnRequestCommand(requireNotNull(warehouseId), items.map { it.toCommand() })
    }
}
