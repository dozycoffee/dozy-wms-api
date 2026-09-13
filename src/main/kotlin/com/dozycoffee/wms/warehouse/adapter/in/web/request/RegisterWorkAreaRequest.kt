package com.dozycoffee.wms.warehouse.adapter.`in`.web.request

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import jakarta.validation.constraints.NotNull

data class RegisterWorkAreaRequest(
    @field:NotNull val areaCode: AreaCode?
) {
    fun toCommand(warehouseId: Long): RegisterWorkAreaCommand {
        return RegisterWorkAreaCommand(warehouseId, requireNotNull(areaCode))
    }
}
