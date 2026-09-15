package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.warehouse.application.port.out.LocationRepository
import com.dozycoffee.wms.warehouse.domain.model.Location
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class LocationPersistenceAdapter(
    private val locationR2dbcRepository: LocationR2dbcRepository
) : LocationRepository {

    override fun save(location: Location): Mono<Location> {
        val entity = LocationEntity.from(location)
        val locationId = location.locationId
            ?: return locationR2dbcRepository.save(entity).map { it.toDomain() }
        return locationR2dbcRepository.findById(locationId)
            .doOnNext { entity.copyAuditFieldsFrom(it) }
            .then(locationR2dbcRepository.save(entity))
            .map { it.toDomain() }
    }

    override fun findById(locationId: Long): Mono<Location> {
        return locationR2dbcRepository.findById(locationId).map { it.toDomain() }
    }

    override fun findByZoneId(zoneId: Long): Flux<Location> {
        return locationR2dbcRepository.findByZoneId(zoneId).map { it.toDomain() }
    }

    override fun delete(location: Location): Mono<Void> {
        return locationR2dbcRepository.delete(LocationEntity.from(location))
    }
}
