package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.global.common.BaseEntity;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.model.Zone;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("zone")
@Getter
public class ZoneEntity extends BaseEntity {

    private static final String STATUS_GROUP = "ZONE_STATUS";

    @Id
    private Long zoneId;
    private Long warehouseId;
    private String zoneCode;
    private String zoneStatus;

    private ZoneEntity() {
    }

    public Zone toDomain() {
        return Zone.reconstitute(
                zoneId,
                warehouseId,
                ZoneCode.valueOf(zoneCode),
                CommonCodes.fromCode(AvailabilityStatus.class, zoneStatus)
        );
    }

    public static ZoneEntity from(Zone domain) {
        ZoneEntity entity = new ZoneEntity();
        entity.zoneId = domain.getZoneId();
        entity.warehouseId = domain.getWarehouseId();
        entity.zoneCode = domain.getZoneCode().name();
        entity.zoneStatus = CommonCodes.toCode(STATUS_GROUP, domain.getZoneStatus());
        return entity;
    }
}
