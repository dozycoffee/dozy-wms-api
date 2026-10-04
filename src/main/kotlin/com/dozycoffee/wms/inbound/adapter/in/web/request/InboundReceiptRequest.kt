package com.dozycoffee.wms.inbound.adapter.`in`.web.request

import com.dozycoffee.wms.inbound.application.port.`in`.command.InboundReceiptCommand
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class InboundReceiptRequest(
    @field:NotBlank val lotNumber: String?,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?,
    @field:NotNull @field:Min(1) val quantity: Int?,
    @field:NotNull val inspectionResult: InspectionResult?,
    val defectReason: DefectReason?
) {
    fun toCommand(): InboundReceiptCommand {
        return InboundReceiptCommand(
            requireNotNull(lotNumber),
            manufactureDate,
            expirationDate,
            requireNotNull(quantity),
            requireNotNull(inspectionResult),
            defectReason
        )
    }
}
