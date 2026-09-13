package com.dozycoffee.wms.warehouse.application.port.`in`.result

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.model.Zone

data class ZoneResult(
    val zoneId: Long,
    val warehouseId: Long,
    val zoneCode: ZoneCode,
    val zoneName: String,
    val temperatureType: TemperatureType,
    val maxCapacity: Int,
    val zoneStatus: AvailabilityStatus
) {
    companion object {
        fun from(zone: Zone): ZoneResult {
            return ZoneResult(
                requireNotNull(zone.zoneId),
                zone.warehouseId,
                zone.zoneCode,
                zone.zoneName,
                zone.temperatureType,
                zone.capacity.value,
                zone.zoneStatus
            )
        }
    }
}
