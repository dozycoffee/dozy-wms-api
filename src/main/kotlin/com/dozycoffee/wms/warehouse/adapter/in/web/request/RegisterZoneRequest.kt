package com.dozycoffee.wms.warehouse.adapter.`in`.web.request

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import jakarta.validation.constraints.NotNull

data class RegisterZoneRequest(
    @field:NotNull val zoneCode: ZoneCode?
) {
    fun toCommand(warehouseId: Long): RegisterZoneCommand {
        return RegisterZoneCommand(warehouseId, requireNotNull(zoneCode))
    }
}
