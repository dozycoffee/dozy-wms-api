package com.dozycoffee.wms.warehouse.application.port.in.result;

import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.WorkArea;

public record WorkAreaResult(
        Long workAreaId,
        Long warehouseId,
        AreaCode areaCode,
        String areaName,
        int maxCapacity,
        int usedCapacity,
        AvailabilityStatus workAreaStatus
) {

    public static WorkAreaResult from(WorkArea workArea) {
        return new WorkAreaResult(
                workArea.getWorkAreaId(),
                workArea.getWarehouseId(),
                workArea.getAreaCode(),
                workArea.getAreaName(),
                workArea.getCapacity().value(),
                workArea.getUsedCapacity(),
                workArea.getWorkAreaStatus()
        );
    }
}
