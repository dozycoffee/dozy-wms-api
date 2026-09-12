package com.dozycoffee.wms.warehouse.adapter.in.web.response;

import com.dozycoffee.wms.warehouse.application.port.in.result.ZoneResult;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;

public record ZoneResponse(
        Long zoneId,
        Long warehouseId,
        ZoneCode zoneCode,
        String zoneName,
        TemperatureType temperatureType,
        int maxCapacity,
        AvailabilityStatus zoneStatus
) {

    public static ZoneResponse from(ZoneResult result) {
        return new ZoneResponse(
                result.zoneId(),
                result.warehouseId(),
                result.zoneCode(),
                result.zoneName(),
                result.temperatureType(),
                result.maxCapacity(),
                result.zoneStatus()
        );
    }
}
