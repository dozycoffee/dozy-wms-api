package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import reactor.core.publisher.Mono

interface GetLocationUseCase {
    fun getById(locationId: Long): Mono<LocationResult>
}
