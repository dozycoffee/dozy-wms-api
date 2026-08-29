package com.dozycoffee.wms.warehouse.domain.model;

import com.dozycoffee.wms.global.common.BaseEntity;
import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.exception.ZoneErrorCode;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
        validateWarehouseId(warehouseId);
        validateZoneCode(zoneCode);
        validateZoneStatus(zoneStatus);
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

    private static void validateWarehouseId(Long warehouseId) {
        if (warehouseId == null) {
            throw new InvalidDomainValueException(ZoneErrorCode.INVALID_WAREHOUSE_ID);
        }
    }

    private static void validateZoneCode(ZoneCode zoneCode) {
        if (zoneCode == null) {
            throw new InvalidDomainValueException(ZoneErrorCode.INVALID_ZONE_CODE);
        }
    }

    private static void validateZoneStatus(AvailabilityStatus zoneStatus) {
        if (zoneStatus == null) {
            throw new InvalidDomainValueException(ZoneErrorCode.INVALID_ZONE_STATUS);
        }
    }
}
