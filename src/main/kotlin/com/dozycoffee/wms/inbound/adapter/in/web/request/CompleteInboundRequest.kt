package com.dozycoffee.wms.inbound.adapter.`in`.web.request

import com.dozycoffee.wms.inbound.application.port.`in`.command.CompleteInboundCommand
import jakarta.validation.Valid

data class CompleteInboundRequest(
    @field:Valid val lotAssignments: List<LotAssignmentRequest> = emptyList()
) {
    fun toCommand(inboundId: Long): CompleteInboundCommand {
        return CompleteInboundCommand(inboundId, lotAssignments.map { it.toCommand() })
    }
}
