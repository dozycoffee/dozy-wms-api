package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.warehouse.application.port.out.LocationRepository
import com.dozycoffee.wms.warehouse.domain.model.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class LocationPersistenceAdapter(
    private val locationR2dbcRepository: LocationR2dbcRepository
) : LocationRepository {

    override suspend fun save(location: Location): Location {
        val entity = LocationEntity.from(location)
        val locationId = location.locationId
        if (locationId != null) {
            locationR2dbcRepository.findById(locationId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return locationR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(locationId: Long): Location? {
        return locationR2dbcRepository.findById(locationId)?.toDomain()
    }

    override fun findByZoneId(zoneId: Long): Flow<Location> {
        return locationR2dbcRepository.findByZoneId(zoneId).map { it.toDomain() }
    }

    override suspend fun delete(location: Location) {
        locationR2dbcRepository.delete(LocationEntity.from(location))
    }
}
