package com.dozycoffee.wms.warehouse.application.service

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
import com.dozycoffee.wms.warehouse.domain.exception.LocationNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Location
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class LocationService(
    private val locationRepository: LocationRepository
) : RegisterLocationUseCase, OccupyLocationUseCase, ReleaseLocationUseCase, GetLocationUseCase {

    @Transactional
    override fun register(command: RegisterLocationCommand): Mono<LocationResult> {
        val location = Location.create(
            command.zoneId,
            command.locationCode,
            command.maxCapacity,
            AvailabilityStatus.AVAILABLE
        )
        return locationRepository.save(location).map { LocationResult.from(it) }
    }

    @Transactional
    override fun occupy(command: OccupyLocationCommand): Mono<LocationResult> {
        return findLocationOrThrow(command.locationId)
            .doOnNext { it.occupy(command.amount) }
            .flatMap { locationRepository.save(it) }
            .map { LocationResult.from(it) }
    }

    @Transactional
    override fun release(command: ReleaseLocationCommand): Mono<LocationResult> {
        return findLocationOrThrow(command.locationId)
            .doOnNext { it.release(command.amount) }
            .flatMap { locationRepository.save(it) }
            .map { LocationResult.from(it) }
    }

    @Transactional(readOnly = true)
    override fun getById(locationId: Long): Mono<LocationResult> {
        return findLocationOrThrow(locationId).map { LocationResult.from(it) }
    }

    @Transactional(readOnly = true)
    override fun getByZoneId(zoneId: Long): Flux<LocationResult> {
        return locationRepository.findByZoneId(zoneId).map { LocationResult.from(it) }
    }

    private fun findLocationOrThrow(locationId: Long): Mono<Location> {
        return locationRepository.findById(locationId)
            .switchIfEmpty(Mono.error(LocationNotFoundException()))
    }
}
