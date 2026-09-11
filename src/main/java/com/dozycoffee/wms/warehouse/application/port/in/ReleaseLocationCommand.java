package com.dozycoffee.wms.warehouse.application.port.in;

public record ReleaseLocationCommand(
        Long locationId,
        int amount
) {
}
