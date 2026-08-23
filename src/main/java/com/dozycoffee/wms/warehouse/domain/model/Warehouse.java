package com.dozycoffee.wms.warehouse.domain.model;

import com.dozycoffee.wms.global.common.SoftDeletableEntity;
import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.enumeration.WarehouseStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode;
import com.dozycoffee.wms.warehouse.domain.valueobject.Address;
import com.dozycoffee.wms.warehouse.domain.valueobject.Coordinate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Warehouse extends SoftDeletableEntity {

    // 창고의 최대 수용 가능 용량
    private static final int MAX_CAPACITY = 1000;

    private final Long warehouseId;
    private final String warehouseName;
    private final Address address;
    private final Coordinate coordinate;
    private WarehouseStatus warehouseStatus;

    public static Warehouse create(
            String warehouseName,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            WarehouseStatus warehouseStatus
    ) {
        validateWarehouseName(warehouseName);
        validateWarehouseStatus(warehouseStatus);
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
            WarehouseStatus warehouseStatus
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

    private static void validateWarehouseStatus(WarehouseStatus warehouseStatus) {
        if (warehouseStatus == null) {
            throw new InvalidDomainValueException(WarehouseErrorCode.INVALID_WAREHOUSE_STATUS);
        }
    }

    public void activate() {
        this.warehouseStatus = WarehouseStatus.AVAILABLE;
    }

    public void deactivate() {
        this.warehouseStatus = WarehouseStatus.UNAVAILABLE;
    }
}
