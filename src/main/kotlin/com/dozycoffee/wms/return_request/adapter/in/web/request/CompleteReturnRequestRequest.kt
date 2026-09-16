package com.dozycoffee.wms.return_request.adapter.`in`.web.request

import com.dozycoffee.wms.return_request.application.port.`in`.command.CompleteReturnRequestCommand
import jakarta.validation.Valid

data class CompleteReturnRequestRequest(
    @field:Valid val lotAssignments: List<ReturnItemLotAssignmentRequest> = emptyList()
) {
    fun toCommand(returnRequestId: Long): CompleteReturnRequestCommand {
        return CompleteReturnRequestCommand(returnRequestId, lotAssignments.map { it.toCommand() })
    }
}
