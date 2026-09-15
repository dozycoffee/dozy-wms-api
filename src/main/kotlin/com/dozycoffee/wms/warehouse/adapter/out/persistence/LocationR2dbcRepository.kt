package com.dozycoffee.wms.warehouse.adapter.out.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface LocationR2dbcRepository : ReactiveCrudRepository<LocationEntity, Long> {
    fun findByZoneId(zoneId: Long): Flux<LocationEntity>
}
