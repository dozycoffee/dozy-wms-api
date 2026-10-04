package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

interface GetZoneUseCase {
    suspend fun getById(zoneId: Long): ZoneResult
    suspend fun getByWarehouseIdAndZoneCode(warehouseId: Long, zoneCode: ZoneCode): ZoneResult
}
