package com.dozycoffee.wms.warehouse.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.exception.ZoneErrorCode
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity

class Zone private constructor(
    val zoneId: Long?,
    val warehouseId: Long,
    val zoneCode: ZoneCode,
    zoneStatus: AvailabilityStatus
) : BaseEntity() {

    var zoneStatus: AvailabilityStatus = zoneStatus
        private set

    val zoneName: String get() = zoneCode.zoneName
    val temperatureType: TemperatureType get() = zoneCode.temperatureType
    val capacity: Capacity get() = zoneCode.capacity

    companion object {
        fun create(
            warehouseId: Long?,
            zoneCode: ZoneCode?,
            zoneStatus: AvailabilityStatus?
        ): Zone {
            val validWarehouseId: Long = requireNonNull(warehouseId, ZoneErrorCode.INVALID_WAREHOUSE_ID)
            val validZoneCode: ZoneCode = requireNonNull(zoneCode, ZoneErrorCode.INVALID_ZONE_CODE)
            val validZoneStatus: AvailabilityStatus = requireNonNull(zoneStatus, ZoneErrorCode.INVALID_ZONE_STATUS)
            return Zone(null, validWarehouseId, validZoneCode, validZoneStatus)
        }

        fun reconstitute(
            zoneId: Long,
            warehouseId: Long,
            zoneCode: ZoneCode,
            zoneStatus: AvailabilityStatus
        ): Zone {
            return Zone(zoneId, warehouseId, zoneCode, zoneStatus)
        }
    }
}
