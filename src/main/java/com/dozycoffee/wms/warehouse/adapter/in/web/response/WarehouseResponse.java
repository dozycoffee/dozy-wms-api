package com.dozycoffee.wms.warehouse.adapter.in.web.response;

import com.dozycoffee.wms.warehouse.application.port.in.result.WarehouseResult;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;

import java.math.BigDecimal;

public record WarehouseResponse(
        Long warehouseId,
        String warehouseName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        AvailabilityStatus warehouseStatus
) {

    public static WarehouseResponse from(WarehouseResult result) {
        return new WarehouseResponse(
                result.warehouseId(),
                result.warehouseName(),
                result.address(),
                result.latitude(),
                result.longitude(),
                result.warehouseStatus()
        );
    }
}
