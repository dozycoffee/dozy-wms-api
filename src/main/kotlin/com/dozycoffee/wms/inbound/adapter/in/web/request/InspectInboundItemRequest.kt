package com.dozycoffee.wms.inbound.adapter.`in`.web.request

import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class InspectInboundItemRequest(
    @field:NotNull @field:Valid val receipts: List<InboundReceiptRequest>?
) {
    fun toCommand(inboundItemId: Long): InspectInboundItemCommand {
        return InspectInboundItemCommand(inboundItemId, requireNotNull(receipts).map { it.toCommand() })
    }
}
