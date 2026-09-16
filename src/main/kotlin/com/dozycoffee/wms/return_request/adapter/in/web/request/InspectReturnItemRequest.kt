package com.dozycoffee.wms.return_request.adapter.`in`.web.request

import com.dozycoffee.wms.return_request.application.port.`in`.command.InspectReturnItemCommand
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class InspectReturnItemRequest(
    @field:NotNull @field:Min(0) val actualQuantity: Int?,
    @field:NotNull val inspectionResult: ReturnInspectionResult?
) {
    fun toCommand(returnItemId: Long): InspectReturnItemCommand {
        return InspectReturnItemCommand(
            returnItemId,
            requireNotNull(actualQuantity),
            requireNotNull(inspectionResult)
        )
    }
}
