package com.dozycoffee.wms.warehouse.adapter.`in`.web.response

import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus

data class LocationResponse(
    val locationId: Long,
    val zoneId: Long,
    val locationCode: String,
    val maxCapacity: Int,
    val usedCapacity: Int,
    val locationStatus: AvailabilityStatus
) {
    companion object {
        fun from(result: LocationResult): LocationResponse {
            return LocationResponse(
                result.locationId,
                result.zoneId,
                result.locationCode,
                result.maxCapacity,
                result.usedCapacity,
                result.locationStatus
            )
        }
    }
}
