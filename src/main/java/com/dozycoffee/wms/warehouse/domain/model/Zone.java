package com.dozycoffee.wms.warehouse.domain.model;

import com.dozycoffee.wms.global.common.BaseEntity;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.exception.ZoneErrorCode;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dozycoffee.wms.global.error.DomainValidator.requireNonNull;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Zone extends BaseEntity {

    private final Long zoneId;
    private final Long warehouseId;
    private final ZoneCode zoneCode;
    private AvailabilityStatus zoneStatus;

    public static Zone create(
            Long warehouseId,
            ZoneCode zoneCode,
            AvailabilityStatus zoneStatus
    ) {
        requireNonNull(warehouseId, ZoneErrorCode.INVALID_WAREHOUSE_ID);
        requireNonNull(zoneCode, ZoneErrorCode.INVALID_ZONE_CODE);
        requireNonNull(zoneStatus, ZoneErrorCode.INVALID_ZONE_STATUS);
        return new Zone(null, warehouseId, zoneCode, zoneStatus);
    }

    public static Zone reconstitute(
            Long zoneId,
            Long warehouseId,
            ZoneCode zoneCode,
            AvailabilityStatus zoneStatus
    ) {
        return new Zone(zoneId, warehouseId, zoneCode, zoneStatus);
    }

    public String getZoneName() {
        return zoneCode.getZoneName();
    }

    public TemperatureType getTemperatureType() {
        return zoneCode.getTemperatureType();
    }

    public Capacity getCapacity() {
        return zoneCode.getCapacity();
    }
}
