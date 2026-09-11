package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.global.common.SoftDeletableEntity;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.Warehouse;
import com.dozycoffee.wms.warehouse.domain.valueobject.Address;
import com.dozycoffee.wms.warehouse.domain.valueobject.Coordinate;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("warehouse")
@Getter
public class WarehouseEntity extends SoftDeletableEntity {

    private static final String STATUS_GROUP = "WAREHOUSE_STATUS";

    @Id
    private Long warehouseId;
    private String warehouseName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String warehouseStatus;

    private WarehouseEntity() {
    }

    public Warehouse toDomain() {
        return Warehouse.reconstitute(
                warehouseId,
                warehouseName,
                new Address(address),
                new Coordinate(latitude, longitude),
                CommonCodes.fromCode(AvailabilityStatus.class, warehouseStatus)
        );
    }

    public static WarehouseEntity from(Warehouse domain) {
        WarehouseEntity entity = new WarehouseEntity();
        entity.warehouseId = domain.getWarehouseId();
        entity.warehouseName = domain.getWarehouseName();
        entity.address = domain.getAddress().value();
        entity.latitude = domain.getCoordinate().latitude();
        entity.longitude = domain.getCoordinate().longitude();
        entity.warehouseStatus = CommonCodes.toCode(STATUS_GROUP, domain.getWarehouseStatus());
        return entity;
    }
}
