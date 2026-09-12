package com.dozycoffee.wms.warehouse.adapter.in.web.request;

import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterLocationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RegisterLocationRequest(
        @NotBlank String locationCode,
        @Positive int maxCapacity
) {

    public RegisterLocationCommand toCommand(Long zoneId) {
        return new RegisterLocationCommand(zoneId, locationCode, maxCapacity);
    }
}
