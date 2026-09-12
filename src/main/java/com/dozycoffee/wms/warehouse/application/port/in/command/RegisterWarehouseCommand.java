package com.dozycoffee.wms.warehouse.application.port.in.command;

import java.math.BigDecimal;

public record RegisterWarehouseCommand(
        String warehouseName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
