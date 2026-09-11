package com.dozycoffee.wms.warehouse.application.port.in;

public record OccupyLocationCommand(
        Long locationId,
        int amount
) {
}
