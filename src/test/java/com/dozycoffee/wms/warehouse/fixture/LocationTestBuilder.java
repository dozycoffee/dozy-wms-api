package com.dozycoffee.wms.warehouse.fixture;

import com.dozycoffee.wms.warehouse.domain.enumeration.LocationStatus;
import com.dozycoffee.wms.warehouse.domain.model.Location;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import com.dozycoffee.wms.warehouse.domain.valueobject.LocationCode;

public class LocationTestBuilder {

    private Long locationId = null;
    private Long zoneId = 1L;
    private String locationCode = "A-01";
    private int maxCapacity = 70;
    private int usedCapacity = 0;
    private LocationStatus locationStatus = LocationStatus.ACTIVE;

    public static LocationTestBuilder location() {
        return new LocationTestBuilder();
    }

    public LocationTestBuilder locationId(Long locationId) {
        this.locationId = locationId;
        return this;
    }

    public LocationTestBuilder zoneId(Long zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    public LocationTestBuilder locationCode(String locationCode) {
        this.locationCode = locationCode;
        return this;
    }

    public LocationTestBuilder maxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        return this;
    }

    public LocationTestBuilder usedCapacity(int usedCapacity) {
        this.usedCapacity = usedCapacity;
        return this;
    }

    public LocationTestBuilder locationStatus(LocationStatus locationStatus) {
        this.locationStatus = locationStatus;
        return this;
    }

    public Location build() {
        if (locationId != null) {
            return Location.reconstitute(
                    locationId,
                    zoneId,
                    new LocationCode(locationCode),
                    new Capacity(maxCapacity),
                    usedCapacity,
                    locationStatus
            );
        }
        return Location.create(
                zoneId,
                locationCode,
                maxCapacity,
                locationStatus
        );
    }
}
