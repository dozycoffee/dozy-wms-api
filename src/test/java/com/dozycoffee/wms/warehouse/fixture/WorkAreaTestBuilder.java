package com.dozycoffee.wms.warehouse.fixture;

import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.WorkAreaStatus;
import com.dozycoffee.wms.warehouse.domain.model.WorkArea;

public class WorkAreaTestBuilder {

    private Long workAreaId = null;
    private Long warehouseId = 1L;
    private AreaCode areaCode = AreaCode.INBOUND;
    private int usedCapacity = 0;
    private WorkAreaStatus workAreaStatus = WorkAreaStatus.ACTIVE;

    public static WorkAreaTestBuilder workArea() {
        return new WorkAreaTestBuilder();
    }

    public WorkAreaTestBuilder workAreaId(Long workAreaId) {
        this.workAreaId = workAreaId;
        return this;
    }

    public WorkAreaTestBuilder warehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public WorkAreaTestBuilder areaCode(AreaCode areaCode) {
        this.areaCode = areaCode;
        return this;
    }

    public WorkAreaTestBuilder usedCapacity(int usedCapacity) {
        this.usedCapacity = usedCapacity;
        return this;
    }

    public WorkAreaTestBuilder workAreaStatus(WorkAreaStatus workAreaStatus) {
        this.workAreaStatus = workAreaStatus;
        return this;
    }

    public WorkArea build() {
        if (workAreaId != null) {
            return WorkArea.reconstitute(
                    workAreaId,
                    warehouseId,
                    areaCode,
                    usedCapacity,
                    workAreaStatus
            );
        }
        return WorkArea.create(
                warehouseId,
                areaCode,
                workAreaStatus
        );
    }
}
