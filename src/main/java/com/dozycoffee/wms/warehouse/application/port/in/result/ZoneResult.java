package com.dozycoffee.wms.warehouse.application.port.in.result;

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.model.Zone;

public record ZoneResult(
        Long zoneId,
        Long warehouseId,
        ZoneCode zoneCode,
        String zoneName,
        TemperatureType temperatureType,
        int maxCapacity,
        AvailabilityStatus zoneStatus
) {

    public static ZoneResult from(Zone zone) {
        return new ZoneResult(
                zone.getZoneId(),
                zone.getWarehouseId(),
                zone.getZoneCode(),
                zone.getZoneName(),
                zone.getTemperatureType(),
                zone.getCapacity().value(),
                zone.getZoneStatus()
        );
    }
}
