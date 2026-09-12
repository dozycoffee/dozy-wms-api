package com.dozycoffee.wms.warehouse.adapter.in.web.response;

import com.dozycoffee.wms.warehouse.application.port.in.result.WorkAreaResult;
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;

public record WorkAreaResponse(
        Long workAreaId,
        Long warehouseId,
        AreaCode areaCode,
        String areaName,
        int maxCapacity,
        int usedCapacity,
        AvailabilityStatus workAreaStatus
) {

    public static WorkAreaResponse from(WorkAreaResult result) {
        return new WorkAreaResponse(
                result.workAreaId(),
                result.warehouseId(),
                result.areaCode(),
                result.areaName(),
                result.maxCapacity(),
                result.usedCapacity(),
                result.workAreaStatus()
        );
    }
}
