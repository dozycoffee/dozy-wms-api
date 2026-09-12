package com.dozycoffee.wms.warehouse.application.port.in.result;

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.Location;

public record LocationResult(
        Long locationId,
        Long zoneId,
        String locationCode,
        int maxCapacity,
        int usedCapacity,
        AvailabilityStatus locationStatus
) {

    public static LocationResult from(Location location) {
        return new LocationResult(
                location.getLocationId(),
                location.getZoneId(),
                location.getLocationCode().value(),
                location.getMaxCapacity().value(),
                location.getUsedCapacity(),
                location.getLocationStatus()
        );
    }
}
