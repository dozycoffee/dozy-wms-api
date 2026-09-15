package com.dozycoffee.wms.inbound.adapter.`in`.web.request

import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class RegisterInboundRequest(
    @field:NotNull val warehouseId: Long?,
    @field:NotNull val expectedArrivalDate: LocalDate?,
    @field:NotEmpty @field:Valid val items: List<RegisterInboundItemRequest> = emptyList()
) {
    fun toCommand(): RegisterInboundCommand {
        return RegisterInboundCommand(
            requireNotNull(warehouseId),
            requireNotNull(expectedArrivalDate),
            items.map { it.toCommand() }
        )
    }
}
