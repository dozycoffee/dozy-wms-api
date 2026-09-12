package com.dozycoffee.wms.warehouse.application.port.in.command;

public record OccupyLocationCommand(
        Long locationId,
        int amount
) {
}
