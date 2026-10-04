package com.dozycoffee.wms.inbound.adapter.`in`.web.request

import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundItemCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class RegisterInboundItemRequest(
    @field:NotNull val productId: Long?,
    @field:Min(1) val expectedQuantity: Int?,
    val expectedLotNumber: String? = null,
    val expectedExpirationDate: LocalDate? = null
) {
    fun toCommand(): RegisterInboundItemCommand {
        return RegisterInboundItemCommand(
            requireNotNull(productId),
            requireNotNull(expectedQuantity),
            expectedLotNumber,
            expectedExpirationDate
        )
    }
}
