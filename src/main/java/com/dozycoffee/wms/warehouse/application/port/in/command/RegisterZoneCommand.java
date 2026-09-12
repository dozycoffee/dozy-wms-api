package com.dozycoffee.wms.warehouse.application.port.in.command;

import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;

public record RegisterZoneCommand(
        Long warehouseId,
        ZoneCode zoneCode
) {
}
