package com.dozycoffee.wms.inventory.adapter.`in`.web.request

import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class RegisterInventoryRequest(
    @field:NotNull val lotId: Long?,
    @field:NotNull val locationId: Long?,
    @field:Min(1) val quantity: Int?,
    @field:NotNull val referenceId: Long?
) {
    fun toCommand(): RegisterInventoryCommand {
        return RegisterInventoryCommand(
            requireNotNull(lotId),
            requireNotNull(locationId),
            requireNotNull(quantity),
            requireNotNull(referenceId)
        )
    }
}
