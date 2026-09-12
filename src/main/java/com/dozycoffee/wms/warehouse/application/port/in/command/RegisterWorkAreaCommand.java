package com.dozycoffee.wms.warehouse.application.port.in.command;

import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;

public record RegisterWorkAreaCommand(
        Long warehouseId,
        AreaCode areaCode
) {
}
