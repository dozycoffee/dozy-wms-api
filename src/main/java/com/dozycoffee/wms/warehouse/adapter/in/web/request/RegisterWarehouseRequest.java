package com.dozycoffee.wms.warehouse.adapter.in.web.request;

import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterWarehouseCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegisterWarehouseRequest(
        @NotBlank String warehouseName,
        @NotBlank String address,
        @NotNull BigDecimal latitude,
        @NotNull BigDecimal longitude
) {

    public RegisterWarehouseCommand toCommand() {
        return new RegisterWarehouseCommand(warehouseName, address, latitude, longitude);
    }
}
