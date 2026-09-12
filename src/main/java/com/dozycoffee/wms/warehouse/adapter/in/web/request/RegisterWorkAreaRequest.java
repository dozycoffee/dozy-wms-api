package com.dozycoffee.wms.warehouse.adapter.in.web.request;

import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterWorkAreaCommand;
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import jakarta.validation.constraints.NotNull;

public record RegisterWorkAreaRequest(
        @NotNull AreaCode areaCode
) {

    public RegisterWorkAreaCommand toCommand(Long warehouseId) {
        return new RegisterWorkAreaCommand(warehouseId, areaCode);
    }
}
