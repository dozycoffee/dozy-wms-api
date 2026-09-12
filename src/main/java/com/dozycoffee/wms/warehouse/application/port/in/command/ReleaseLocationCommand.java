package com.dozycoffee.wms.warehouse.application.port.in.command;

public record ReleaseLocationCommand(
        Long locationId,
        int amount
) {
}
