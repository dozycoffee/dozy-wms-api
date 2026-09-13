package com.dozycoffee.wms.warehouse.adapter.`in`.web.response

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus

data class WorkAreaResponse(
    val workAreaId: Long,
    val warehouseId: Long,
    val areaCode: AreaCode,
    val areaName: String,
    val maxCapacity: Int,
    val usedCapacity: Int,
    val workAreaStatus: AvailabilityStatus
) {
    companion object {
        fun from(result: WorkAreaResult): WorkAreaResponse {
            return WorkAreaResponse(
                result.workAreaId,
                result.warehouseId,
                result.areaCode,
                result.areaName,
                result.maxCapacity,
                result.usedCapacity,
                result.workAreaStatus
            )
        }
    }
}
