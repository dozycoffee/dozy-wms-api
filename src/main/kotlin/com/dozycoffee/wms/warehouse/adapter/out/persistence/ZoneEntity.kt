package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.model.Zone
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("zone")
class ZoneEntity private constructor() : BaseEntity() {

    @Id
    var zoneId: Long? = null
        private set

    var warehouseId: Long? = null
        private set

    var zoneCode: String? = null
        private set

    var zoneStatus: String? = null
        private set

    fun toDomain(): Zone {
        return Zone.reconstitute(
            requireNotNull(zoneId),
            requireNotNull(warehouseId),
            ZoneCode.valueOf(requireNotNull(zoneCode)),
            CommonCodes.fromCode(AvailabilityStatus::class.java, requireNotNull(zoneStatus))
        )
    }

    companion object {
        private const val STATUS_GROUP = "ZONE_STATUS"

        fun from(domain: Zone): ZoneEntity {
            val entity = ZoneEntity()
            entity.zoneId = domain.zoneId
            entity.warehouseId = domain.warehouseId
            entity.zoneCode = domain.zoneCode.name
            entity.zoneStatus = CommonCodes.toCode(STATUS_GROUP, domain.zoneStatus)
            return entity
        }
    }
}
