package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Location
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity
import com.dozycoffee.wms.warehouse.domain.valueobject.LocationCode
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("location")
class LocationEntity private constructor() : BaseEntity() {

    @Id
    var locationId: Long? = null
        private set

    var zoneId: Long? = null
        private set

    var locationCode: String? = null
        private set

    var maxCapacity: Int = 0
        private set

    var usedCapacity: Int = 0
        private set

    var locationStatus: String? = null
        private set

    fun toDomain(): Location {
        return Location.reconstitute(
            requireNotNull(locationId),
            requireNotNull(zoneId),
            LocationCode.of(locationCode),
            Capacity(maxCapacity),
            usedCapacity,
            CommonCodes.fromCode(AvailabilityStatus::class.java, requireNotNull(locationStatus))
        )
    }

    companion object {
        private const val STATUS_GROUP = "LOCATION_STATUS"

        fun from(domain: Location): LocationEntity {
            val entity = LocationEntity()
            entity.locationId = domain.locationId
            entity.zoneId = domain.zoneId
            entity.locationCode = domain.locationCode.value
            entity.maxCapacity = domain.maxCapacity.value
            entity.usedCapacity = domain.usedCapacity
            entity.locationStatus = CommonCodes.toCode(STATUS_GROUP, domain.locationStatus)
            return entity
        }
    }
}
