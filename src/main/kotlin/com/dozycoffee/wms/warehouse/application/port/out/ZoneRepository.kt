package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.model.Zone

interface ZoneRepository {
    suspend fun save(zone: Zone): Zone
    suspend fun findById(zoneId: Long): Zone?
    suspend fun findByWarehouseIdAndZoneCode(warehouseId: Long, zoneCode: ZoneCode): Zone?
    suspend fun delete(zone: Zone)
}
