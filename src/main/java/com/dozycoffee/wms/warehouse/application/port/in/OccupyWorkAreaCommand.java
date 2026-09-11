package com.dozycoffee.wms.warehouse.application.port.in;

public record OccupyWorkAreaCommand(
        Long workAreaId,
        int amount
) {
}
