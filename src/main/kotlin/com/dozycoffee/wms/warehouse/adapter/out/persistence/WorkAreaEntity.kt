package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
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
            CommonCodes.fromCode(AreaCode::class.java, requireNotNull(areaCode)),
            usedCapacity,
            CommonCodes.fromCode(AvailabilityStatus::class.java, requireNotNull(workAreaStatus))
        )
    }

    companion object {
        private const val AREA_CODE_GROUP = "WORK_AREA_TYPE"
        private const val STATUS_GROUP = "WORK_AREA_STATUS"

        fun from(domain: WorkArea): WorkAreaEntity {
            val entity = WorkAreaEntity()
            entity.workAreaId = domain.workAreaId
            entity.warehouseId = domain.warehouseId
            entity.areaCode = CommonCodes.toCode(AREA_CODE_GROUP, domain.areaCode)
            entity.usedCapacity = domain.usedCapacity
            entity.workAreaStatus = CommonCodes.toCode(STATUS_GROUP, domain.workAreaStatus)
            return entity
        }
    }
}
