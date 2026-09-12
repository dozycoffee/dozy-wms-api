package com.dozycoffee.wms.warehouse.adapter.in.web.request;

import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterZoneCommand;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import jakarta.validation.constraints.NotNull;

public record RegisterZoneRequest(
        @NotNull ZoneCode zoneCode
) {

    public RegisterZoneCommand toCommand(Long warehouseId) {
        return new RegisterZoneCommand(warehouseId, zoneCode);
    }
}
