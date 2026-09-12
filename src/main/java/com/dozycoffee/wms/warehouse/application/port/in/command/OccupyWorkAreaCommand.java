package com.dozycoffee.wms.warehouse.application.port.in.command;

public record OccupyWorkAreaCommand(
        Long workAreaId,
        int amount
) {
}
