package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository
import com.dozycoffee.wms.warehouse.domain.model.Zone
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ZonePersistenceAdapter(
    private val zoneR2dbcRepository: ZoneR2dbcRepository
) : ZoneRepository {

    override fun save(zone: Zone): Mono<Zone> {
        val entity = ZoneEntity.from(zone)
        val zoneId = zone.zoneId
            ?: return zoneR2dbcRepository.save(entity).map { it.toDomain() }
        return zoneR2dbcRepository.findById(zoneId)
            .doOnNext { entity.copyAuditFieldsFrom(it) }
            .then(zoneR2dbcRepository.save(entity))
            .map { it.toDomain() }
    }

    override fun findById(zoneId: Long): Mono<Zone> {
        return zoneR2dbcRepository.findById(zoneId).map { it.toDomain() }
    }

    override fun delete(zone: Zone): Mono<Void> {
        return zoneR2dbcRepository.delete(ZoneEntity.from(zone))
    }
}
