package com.dozycoffee.wms.disposal.adapter.`in`.web.request

import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class RegisterDisposalRequest(
    @field:NotNull val warehouseId: Long?,
    @field:NotEmpty @field:Valid val items: List<RegisterDisposalItemRequest> = emptyList()
) {
    fun toCommand(): RegisterDisposalCommand {
        return RegisterDisposalCommand(requireNotNull(warehouseId), items.map { it.toCommand() })
    }
}
