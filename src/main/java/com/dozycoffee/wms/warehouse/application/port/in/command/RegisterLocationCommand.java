package com.dozycoffee.wms.warehouse.application.port.in.command;

public record RegisterLocationCommand(
        Long zoneId,
        String locationCode,
        int maxCapacity
) {
}
