package com.dozycoffee.wms.warehouse.adapter.`in`.web.request

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWarehouseCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class RegisterWarehouseRequest(
    @field:NotBlank val warehouseName: String?,
    @field:NotBlank val address: String?,
    @field:NotNull val latitude: BigDecimal?,
    @field:NotNull val longitude: BigDecimal?
) {
    fun toCommand(): RegisterWarehouseCommand {
        return RegisterWarehouseCommand(
            requireNotNull(warehouseName),
            requireNotNull(address),
            requireNotNull(latitude),
            requireNotNull(longitude)
        )
    }
}
