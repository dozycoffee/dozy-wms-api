package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.model.Location
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface LocationRepository {
    fun save(location: Location): Mono<Location>
    fun findById(locationId: Long): Mono<Location>
    fun findByZoneId(zoneId: Long): Flux<Location>
    fun delete(location: Location): Mono<Void>
}
