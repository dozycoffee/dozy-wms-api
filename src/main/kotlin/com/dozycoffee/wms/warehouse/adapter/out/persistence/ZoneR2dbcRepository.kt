package com.dozycoffee.wms.warehouse.adapter.out.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ZoneR2dbcRepository : ReactiveCrudRepository<ZoneEntity, Long> {
    fun findByWarehouseIdAndZoneCode(warehouseId: Long, zoneCode: String): Mono<ZoneEntity>
}
