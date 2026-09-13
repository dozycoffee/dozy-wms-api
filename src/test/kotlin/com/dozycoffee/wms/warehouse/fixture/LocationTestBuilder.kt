package com.dozycoffee.wms.warehouse.fixture

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Location
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity
import com.dozycoffee.wms.warehouse.domain.valueobject.LocationCode

class LocationTestBuilder {

    private var locationId: Long? = null
    private var zoneId: Long? = 1L
    private var locationCode: String? = "A-01"
    private var maxCapacity: Int = 70
    private var usedCapacity: Int = 0
    private var locationStatus: AvailabilityStatus? = AvailabilityStatus.AVAILABLE

    companion object {
        fun location(): LocationTestBuilder = LocationTestBuilder()
    }

    fun locationId(locationId: Long?): LocationTestBuilder {
        this.locationId = locationId
        return this
    }

    fun zoneId(zoneId: Long?): LocationTestBuilder {
        this.zoneId = zoneId
        return this
    }

    fun locationCode(locationCode: String?): LocationTestBuilder {
        this.locationCode = locationCode
        return this
    }

    fun maxCapacity(maxCapacity: Int): LocationTestBuilder {
        this.maxCapacity = maxCapacity
        return this
    }

    fun usedCapacity(usedCapacity: Int): LocationTestBuilder {
        this.usedCapacity = usedCapacity
        return this
    }

    fun locationStatus(locationStatus: AvailabilityStatus?): LocationTestBuilder {
        this.locationStatus = locationStatus
        return this
    }

    fun build(): Location {
        val id = locationId
        if (id != null) {
            return Location.reconstitute(
                id,
                requireNotNull(zoneId),
                LocationCode.of(locationCode),
                Capacity(maxCapacity),
                usedCapacity,
                requireNotNull(locationStatus)
            )
        }
        return Location.create(zoneId, locationCode, maxCapacity, locationStatus)
    }
}
