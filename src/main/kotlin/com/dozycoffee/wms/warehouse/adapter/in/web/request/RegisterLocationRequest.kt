package com.dozycoffee.wms.warehouse.adapter.`in`.web.request

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterLocationCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class RegisterLocationRequest(
    @field:NotBlank val locationCode: String?,
    @field:Positive val maxCapacity: Int
) {
    fun toCommand(zoneId: Long): RegisterLocationCommand {
        return RegisterLocationCommand(zoneId, requireNotNull(locationCode), maxCapacity)
    }
}
