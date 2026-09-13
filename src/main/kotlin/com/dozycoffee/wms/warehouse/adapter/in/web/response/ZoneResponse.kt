package com.dozycoffee.wms.warehouse.adapter.`in`.web.response

import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

data class ZoneResponse(
    val zoneId: Long,
    val warehouseId: Long,
    val zoneCode: ZoneCode,
    val zoneName: String,
    val temperatureType: TemperatureType,
    val maxCapacity: Int,
    val zoneStatus: AvailabilityStatus
) {
    companion object {
        fun from(result: ZoneResult): ZoneResponse {
            return ZoneResponse(
                result.zoneId,
                result.warehouseId,
                result.zoneCode,
                result.zoneName,
                result.temperatureType,
                result.maxCapacity,
                result.zoneStatus
            )
        }
    }
}
