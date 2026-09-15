package com.dozycoffee.wms.inbound.adapter.`in`.web.request

import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class InspectInboundItemRequest(
    @field:NotNull @field:Min(0) val actualQuantity: Int?,
    @field:NotNull val inspectionResult: InspectionResult?
) {
    fun toCommand(inboundItemId: Long): InspectInboundItemCommand {
        return InspectInboundItemCommand(
            inboundItemId,
            requireNotNull(actualQuantity),
            requireNotNull(inspectionResult)
        )
    }
}
