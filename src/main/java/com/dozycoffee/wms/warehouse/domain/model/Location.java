package com.dozycoffee.wms.warehouse.domain.model;

import com.dozycoffee.wms.global.common.BaseEntity;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.InactiveLocationException;
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException;
import com.dozycoffee.wms.warehouse.domain.exception.InvalidLocationAmountException;
import com.dozycoffee.wms.warehouse.domain.exception.LocationCapacityExceededException;
import com.dozycoffee.wms.warehouse.domain.exception.LocationErrorCode;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import com.dozycoffee.wms.warehouse.domain.valueobject.LocationCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dozycoffee.wms.global.error.DomainValidator.requireNonNull;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Location extends BaseEntity {

    private static final int INITIAL_USED_CAPACITY = 0;

    private final Long locationId;
    private final Long zoneId;
    private final LocationCode locationCode;
    private final Capacity maxCapacity;
    private int usedCapacity;
    private AvailabilityStatus locationStatus;

    public static Location create(
            Long zoneId,
            String locationCode,
            int maxCapacity,
            AvailabilityStatus locationStatus
    ) {
        requireNonNull(zoneId, LocationErrorCode.INVALID_ZONE_ID);
        requireNonNull(locationStatus, LocationErrorCode.INVALID_LOCATION_STATUS);
        return new Location(
                null,
                zoneId,
                new LocationCode(locationCode),
                new Capacity(maxCapacity),
                INITIAL_USED_CAPACITY,
                locationStatus
        );
    }

    public static Location reconstitute(
            Long locationId,
            Long zoneId,
            LocationCode locationCode,
            Capacity maxCapacity,
            int usedCapacity,
            AvailabilityStatus locationStatus
    ) {
        return new Location(locationId, zoneId, locationCode, maxCapacity, usedCapacity, locationStatus);
    }

    /** 재고 적재 */
    public void occupy(int amount) {
        validateAmount(amount);
        validateActive();
        validateCapacityNotExceeded(amount);
        this.usedCapacity += amount;
    }

    /** 재고 반출 */
    public void release(int amount) {
        validateAmount(amount);
        validateActive();
        validateSufficientUsedCapacity(amount);
        this.usedCapacity -= amount;
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new InvalidLocationAmountException();
        }
    }

    private void validateActive() {
        if (locationStatus != AvailabilityStatus.AVAILABLE) {
            throw new InactiveLocationException();
        }
    }

    private void validateCapacityNotExceeded(int amount) {
        if (usedCapacity + amount > maxCapacity.value()) {
            throw new LocationCapacityExceededException();
        }
    }

    private void validateSufficientUsedCapacity(int amount) {
        if (usedCapacity - amount < 0) {
            throw new InsufficientLocationCapacityException();
        }
    }
}
