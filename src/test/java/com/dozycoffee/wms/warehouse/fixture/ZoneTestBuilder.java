package com.dozycoffee.wms.warehouse.fixture;

import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneStatus;
import com.dozycoffee.wms.warehouse.domain.model.Zone;

public class ZoneTestBuilder {

    private Long zoneId = null;
    private Long warehouseId = 1L;
    private ZoneCode zoneCode = ZoneCode.A;
    private ZoneStatus zoneStatus = ZoneStatus.ACTIVE;

    public static ZoneTestBuilder zone() {
        return new ZoneTestBuilder();
    }

    public ZoneTestBuilder zoneId(Long zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    public ZoneTestBuilder warehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public ZoneTestBuilder zoneCode(ZoneCode zoneCode) {
        this.zoneCode = zoneCode;
        return this;
    }

    public ZoneTestBuilder zoneStatus(ZoneStatus zoneStatus) {
        this.zoneStatus = zoneStatus;
        return this;
    }

    public Zone build() {
        if (zoneId != null) {
            return Zone.reconstitute(
                    zoneId,
                    warehouseId,
                    zoneCode,
                    zoneStatus
            );
        }
        return Zone.create(
                warehouseId,
                zoneCode,
                zoneStatus
        );
    }
}
