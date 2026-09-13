package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.model.Zone
import reactor.core.publisher.Mono

interface ZoneRepository {
    fun save(zone: Zone): Mono<Zone>
    fun findById(zoneId: Long): Mono<Zone>
    fun delete(zone: Zone): Mono<Void>
}
