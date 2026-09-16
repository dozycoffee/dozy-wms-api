package com.dozycoffee.wms.return_request.adapter.`in`.web.request

import com.dozycoffee.wms.return_request.application.port.`in`.command.ReturnItemLotAssignmentCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class ReturnItemLotAssignmentRequest(
    @field:NotNull val returnItemId: Long?,
    @field:NotBlank val lotNumber: String?,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?
) {
    fun toCommand(): ReturnItemLotAssignmentCommand {
        return ReturnItemLotAssignmentCommand(
            requireNotNull(returnItemId),
            requireNotNull(lotNumber),
            manufactureDate,
            expirationDate
        )
    }
}
