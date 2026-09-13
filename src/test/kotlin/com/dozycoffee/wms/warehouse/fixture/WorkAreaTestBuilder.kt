package com.dozycoffee.wms.warehouse.fixture

import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.WorkArea

class WorkAreaTestBuilder {

    private var workAreaId: Long? = null
    private var warehouseId: Long? = 1L
    private var areaCode: AreaCode? = AreaCode.INBOUND
    private var usedCapacity: Int = 0
    private var workAreaStatus: AvailabilityStatus? = AvailabilityStatus.AVAILABLE

    companion object {
        fun workArea(): WorkAreaTestBuilder = WorkAreaTestBuilder()
    }

    fun workAreaId(workAreaId: Long?): WorkAreaTestBuilder {
        this.workAreaId = workAreaId
        return this
    }

    fun warehouseId(warehouseId: Long?): WorkAreaTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun areaCode(areaCode: AreaCode?): WorkAreaTestBuilder {
        this.areaCode = areaCode
        return this
    }

    fun usedCapacity(usedCapacity: Int): WorkAreaTestBuilder {
        this.usedCapacity = usedCapacity
        return this
    }

    fun workAreaStatus(workAreaStatus: AvailabilityStatus?): WorkAreaTestBuilder {
        this.workAreaStatus = workAreaStatus
        return this
    }

    fun build(): WorkArea {
        val id = workAreaId
        if (id != null) {
            return WorkArea.reconstitute(
                id,
                requireNotNull(warehouseId),
                requireNotNull(areaCode),
                usedCapacity,
                requireNotNull(workAreaStatus)
            )
        }
        return WorkArea.create(warehouseId, areaCode, workAreaStatus)
    }
}
