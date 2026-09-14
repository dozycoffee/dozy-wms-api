package com.dozycoffee.wms.inventory.adapter.`in`.web.request

import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterLotCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class RegisterLotRequest(
    @field:NotBlank val lotNumber: String?,
    @field:NotNull val productId: Long?,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?
) {
    fun toCommand(): RegisterLotCommand {
        return RegisterLotCommand(
            requireNotNull(lotNumber),
            requireNotNull(productId),
            manufactureDate,
            expirationDate
        )
    }
}
