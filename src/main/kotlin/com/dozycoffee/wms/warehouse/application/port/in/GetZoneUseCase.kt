package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import reactor.core.publisher.Mono

interface GetZoneUseCase {
    fun getById(zoneId: Long): Mono<ZoneResult>
}
