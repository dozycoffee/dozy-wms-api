package com.dozycoffee.wms.warehouse.domain.model;

import com.dozycoffee.wms.global.common.SoftDeletableEntity;
import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode;
import com.dozycoffee.wms.warehouse.domain.valueobject.Address;
import com.dozycoffee.wms.warehouse.domain.valueobject.Coordinate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

import static com.dozycoffee.wms.global.error.DomainValidator.requireNonNull;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Warehouse extends SoftDeletableEntity {

    // 창고의 최대 수용 가능 용량
    private static final int MAX_CAPACITY = 1000;

    private final Long warehouseId;
    private final String warehouseName;
    private final Address address;
    private final Coordinate coordinate;
    private AvailabilityStatus warehouseStatus;

    public static Warehouse create(
            String warehouseName,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            AvailabilityStatus warehouseStatus
    ) {
        validateWarehouseName(warehouseName);
        requireNonNull(warehouseStatus, WarehouseErrorCode.INVALID_WAREHOUSE_STATUS);
        return new Warehouse(
                null,
                warehouseName,
                new Address(address),
                new Coordinate(latitude, longitude),
                warehouseStatus
        );
    }

    public static Warehouse reconstitute(
            Long warehouseId,
            String warehouseName,
            Address address,
            Coordinate coordinate,
            AvailabilityStatus warehouseStatus
    ) {
        return new Warehouse(
                warehouseId,
                warehouseName,
                address,
                coordinate,
                warehouseStatus
        );
    }

    private static void validateWarehouseName(String warehouseName) {
        if (warehouseName == null || warehouseName.isBlank()) {
            throw new InvalidDomainValueException(WarehouseErrorCode.INVALID_WAREHOUSE_NAME);
        }
    }

    public void activate() {
        this.warehouseStatus = AvailabilityStatus.AVAILABLE;
    }

    public void deactivate() {
        this.warehouseStatus = AvailabilityStatus.UNAVAILABLE;
    }
}
