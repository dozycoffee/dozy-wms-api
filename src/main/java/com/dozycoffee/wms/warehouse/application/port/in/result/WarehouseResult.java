package com.dozycoffee.wms.warehouse.application.port.in.result;

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.Warehouse;

import java.math.BigDecimal;

public record WarehouseResult(
        Long warehouseId,
        String warehouseName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        AvailabilityStatus warehouseStatus
) {

    public static WarehouseResult from(Warehouse warehouse) {
        return new WarehouseResult(
                warehouse.getWarehouseId(),
                warehouse.getWarehouseName(),
                warehouse.getAddress().value(),
                warehouse.getCoordinate().latitude(),
                warehouse.getCoordinate().longitude(),
                warehouse.getWarehouseStatus()
        );
    }
}
