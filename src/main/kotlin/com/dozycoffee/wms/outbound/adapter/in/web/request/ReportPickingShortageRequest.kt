package com.dozycoffee.wms.outbound.adapter.`in`.web.request

import com.dozycoffee.wms.outbound.application.port.`in`.command.ReportPickingShortageCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class ReportPickingShortageRequest(
    @field:NotEmpty @field:Valid val shortages: List<ShortageRequest>?
) {
    data class ShortageRequest(
        @field:NotNull val outboundAllocationId: Long?,
        @field:NotNull @field:Positive val shortageQuantity: Int?
    )

    fun toCommand(outboundId: Long): ReportPickingShortageCommand {
        return ReportPickingShortageCommand(
            outboundId,
            requireNotNull(shortages).map {
                ReportPickingShortageCommand.Shortage(requireNotNull(it.outboundAllocationId), requireNotNull(it.shortageQuantity))
            }
        )
    }
}
