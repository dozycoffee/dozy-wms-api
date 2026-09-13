package com.dozycoffee.wms.warehouse.application.port.`in`.result

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Location

data class LocationResult(
    val locationId: Long,
    val zoneId: Long,
    val locationCode: String,
    val maxCapacity: Int,
    val usedCapacity: Int,
    val locationStatus: AvailabilityStatus
) {
    companion object {
        fun from(location: Location): LocationResult {
            return LocationResult(
                requireNotNull(location.locationId),
                location.zoneId,
                location.locationCode.value,
                location.maxCapacity.value,
                location.usedCapacity,
                location.locationStatus
            )
        }
    }
}
