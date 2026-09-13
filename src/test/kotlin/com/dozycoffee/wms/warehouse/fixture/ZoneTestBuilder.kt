package com.dozycoffee.wms.warehouse.fixture

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.model.Zone

class ZoneTestBuilder {

    private var zoneId: Long? = null
    private var warehouseId: Long? = 1L
    private var zoneCode: ZoneCode? = ZoneCode.A
    private var zoneStatus: AvailabilityStatus? = AvailabilityStatus.AVAILABLE

    companion object {
        fun zone(): ZoneTestBuilder = ZoneTestBuilder()
    }

    fun zoneId(zoneId: Long?): ZoneTestBuilder {
        this.zoneId = zoneId
        return this
    }

    fun warehouseId(warehouseId: Long?): ZoneTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun zoneCode(zoneCode: ZoneCode?): ZoneTestBuilder {
        this.zoneCode = zoneCode
        return this
    }

    fun zoneStatus(zoneStatus: AvailabilityStatus?): ZoneTestBuilder {
        this.zoneStatus = zoneStatus
        return this
    }

    fun build(): Zone {
        val id = zoneId
        if (id != null) {
            return Zone.reconstitute(
                id,
                requireNotNull(warehouseId),
                requireNotNull(zoneCode),
                requireNotNull(zoneStatus)
            )
        }
        return Zone.create(warehouseId, zoneCode, zoneStatus)
    }
}
