package com.dozycoffee.wms.warehouse.application.port.in.command;

public record ReleaseWorkAreaCommand(
        Long workAreaId,
        int amount
) {
}
