package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("work_area")
class WorkAreaEntity private constructor() : BaseEntity() {

    @Id
    var workAreaId: Long? = null
        private set

    var warehouseId: Long? = null
        private set

    var areaCode: String? = null
        private set

    var usedCapacity: Int = 0
        private set

    var workAreaStatus: String? = null
        private set

    fun toDomain(): WorkArea {
        return WorkArea.reconstitute(
            requireNotNull(workAreaId),
            requireNotNull(warehouseId),
            AreaCode.valueOf(requireNotNull(areaCode)),
            usedCapacity,
            AvailabilityStatus.valueOf(requireNotNull(workAreaStatus))
        )
    }

    companion object {
        fun from(domain: WorkArea): WorkAreaEntity {
            val entity = WorkAreaEntity()
            entity.workAreaId = domain.workAreaId
            entity.warehouseId = domain.warehouseId
            entity.areaCode = domain.areaCode.name
            entity.usedCapacity = domain.usedCapacity
            entity.workAreaStatus = domain.workAreaStatus.name
            return entity
        }
    }
}
