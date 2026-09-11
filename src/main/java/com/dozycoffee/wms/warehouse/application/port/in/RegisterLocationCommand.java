package com.dozycoffee.wms.warehouse.application.port.in;

public record RegisterLocationCommand(
        Long zoneId,
        String locationCode,
        int maxCapacity
) {
}
