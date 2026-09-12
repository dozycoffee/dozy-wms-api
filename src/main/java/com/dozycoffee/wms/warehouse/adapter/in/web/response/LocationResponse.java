package com.dozycoffee.wms.warehouse.adapter.in.web.response;

import com.dozycoffee.wms.warehouse.application.port.in.result.LocationResult;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;

public record LocationResponse(
        Long locationId,
        Long zoneId,
        String locationCode,
        int maxCapacity,
        int usedCapacity,
        AvailabilityStatus locationStatus
) {

    public static LocationResponse from(LocationResult result) {
        return new LocationResponse(
                result.locationId(),
                result.zoneId(),
                result.locationCode(),
                result.maxCapacity(),
                result.usedCapacity(),
                result.locationStatus()
        );
    }
}
