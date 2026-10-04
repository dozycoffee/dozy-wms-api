package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.global.persistence.translatingDuplicateKey
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.application.port.out.LocationRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.DuplicateLocationCodeException
import com.dozycoffee.wms.warehouse.domain.exception.LocationNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LocationService(
    private val locationRepository: LocationRepository
) : RegisterLocationUseCase, OccupyLocationUseCase, ReleaseLocationUseCase, GetLocationUseCase {

    @Transactional
    override suspend fun register(command: RegisterLocationCommand): LocationResult {
        val location = Location.create(
            command.zoneId,
            command.locationCode,
            command.maxCapacity,
            AvailabilityStatus.AVAILABLE
        )
        return LocationResult.from(translatingDuplicateKey({ DuplicateLocationCodeException() }) { locationRepository.save(location) })
    }

    @Transactional
    override suspend fun occupy(command: OccupyLocationCommand): LocationResult {
        val location = findLocationOrThrow(command.locationId)
        location.occupy(command.amount)
        return LocationResult.from(locationRepository.save(location))
    }

    @Transactional
    override suspend fun release(command: ReleaseLocationCommand): LocationResult {
        val location = findLocationOrThrow(command.locationId)
        location.release(command.amount)
        return LocationResult.from(locationRepository.save(location))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(locationId: Long): LocationResult {
        return LocationResult.from(findLocationOrThrow(locationId))
    }

    @Transactional(readOnly = true)
    override fun getByZoneId(zoneId: Long): Flow<LocationResult> {
        return locationRepository.findByZoneId(zoneId).map { LocationResult.from(it) }
    }

    private suspend fun findLocationOrThrow(locationId: Long): Location {
        return locationRepository.findById(locationId) ?: throw LocationNotFoundException()
    }
}
