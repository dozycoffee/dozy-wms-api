package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.global.common.BaseEntity;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.Location;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import com.dozycoffee.wms.warehouse.domain.valueobject.LocationCode;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("location")
@Getter
public class LocationEntity extends BaseEntity {

    private static final String STATUS_GROUP = "LOCATION_STATUS";

    @Id
    private Long locationId;
    private Long zoneId;
    private String locationCode;
    private int maxCapacity;
    private int usedCapacity;
    private String locationStatus;

    private LocationEntity() {
    }

    public Location toDomain() {
        return Location.reconstitute(
                locationId,
                zoneId,
                new LocationCode(locationCode),
                new Capacity(maxCapacity),
                usedCapacity,
                CommonCodes.fromCode(AvailabilityStatus.class, locationStatus)
        );
    }

    public static LocationEntity from(Location domain) {
        LocationEntity entity = new LocationEntity();
        entity.locationId = domain.getLocationId();
        entity.zoneId = domain.getZoneId();
        entity.locationCode = domain.getLocationCode().value();
        entity.maxCapacity = domain.getMaxCapacity().value();
        entity.usedCapacity = domain.getUsedCapacity();
        entity.locationStatus = CommonCodes.toCode(STATUS_GROUP, domain.getLocationStatus());
        return entity;
    }
}
