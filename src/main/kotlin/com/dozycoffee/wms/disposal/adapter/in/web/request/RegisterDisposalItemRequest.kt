package com.dozycoffee.wms.disposal.adapter.`in`.web.request

import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import jakarta.validation.constraints.NotNull

data class RegisterDisposalItemRequest(
    @field:NotNull val locationStockId: Long?,
    @field:NotNull val quantity: Int?,
    @field:NotNull val reason: DisposalReason?
) {
    fun toCommand(): RegisterDisposalItemCommand {
        return RegisterDisposalItemCommand(
            requireNotNull(locationStockId),
            requireNotNull(quantity),
            requireNotNull(reason)
        )
    }
}
