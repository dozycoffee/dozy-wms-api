package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import reactor.core.publisher.Mono

interface GetZoneUseCase {
    fun getById(zoneId: Long): Mono<ZoneResult>
    fun getByWarehouseIdAndZoneCode(warehouseId: Long, zoneCode: ZoneCode): Mono<ZoneResult>
}
