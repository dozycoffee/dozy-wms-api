package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;

public record RegisterZoneCommand(
        Long warehouseId,
        ZoneCode zoneCode
) {
}
