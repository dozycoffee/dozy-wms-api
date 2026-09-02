package com.dozycoffee.wms.warehouse.fixture;

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.Warehouse;
import com.dozycoffee.wms.warehouse.domain.valueobject.Address;
import com.dozycoffee.wms.warehouse.domain.valueobject.Coordinate;

public class WarehouseTestBuilder {

    private Long warehouseId = null;
    private String warehouseName = "도지하우스 제주 센터";
    private Address address = new Address("제주특별자치도 제주시");
    private Coordinate coordinate = Coordinate.of(33.4996, 126.5312);
    private AvailabilityStatus warehouseStatus = AvailabilityStatus.AVAILABLE;

    public static WarehouseTestBuilder warehouse() {
        return new WarehouseTestBuilder();
    }

    public WarehouseTestBuilder warehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public WarehouseTestBuilder warehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
        return this;
    }

    public WarehouseTestBuilder address(String address) {
        this.address = new Address(address);
        return this;
    }

    public WarehouseTestBuilder coordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
        return this;
    }

    public WarehouseTestBuilder warehouseStatus(AvailabilityStatus warehouseStatus) {
        this.warehouseStatus = warehouseStatus;
        return this;
    }

    public Warehouse build() {
        if (warehouseId != null) {
            return Warehouse.reconstitute(
                    warehouseId,
                    warehouseName,
                    address,
                    coordinate,
                    warehouseStatus
            );
        }
        return Warehouse.create(
                warehouseName,
                address.value(),
                coordinate.latitude(),
                coordinate.longitude(),
                warehouseStatus
        );
    }
}
