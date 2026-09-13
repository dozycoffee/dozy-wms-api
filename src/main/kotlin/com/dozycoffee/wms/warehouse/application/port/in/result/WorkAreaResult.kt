package com.dozycoffee.wms.warehouse.application.port.`in`.result

import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.WorkArea

data class WorkAreaResult(
    val workAreaId: Long,
    val warehouseId: Long,
    val areaCode: AreaCode,
    val areaName: String,
    val maxCapacity: Int,
    val usedCapacity: Int,
    val workAreaStatus: AvailabilityStatus
) {
    companion object {
        fun from(workArea: WorkArea): WorkAreaResult {
            return WorkAreaResult(
                requireNotNull(workArea.workAreaId),
                workArea.warehouseId,
                workArea.areaCode,
                workArea.areaName,
                workArea.capacity.value,
                workArea.usedCapacity,
                workArea.workAreaStatus
            )
        }
    }
}
