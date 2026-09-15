package com.dozycoffee.wms.inbound.adapter.`in`.web.request

import com.dozycoffee.wms.inbound.application.port.`in`.command.LotAssignmentCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class LotAssignmentRequest(
    @field:NotNull val inboundItemId: Long?,
    @field:NotBlank val lotNumber: String?,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?
) {
    fun toCommand(): LotAssignmentCommand {
        return LotAssignmentCommand(
            requireNotNull(inboundItemId),
            requireNotNull(lotNumber),
            manufactureDate,
            expirationDate
        )
    }
}
