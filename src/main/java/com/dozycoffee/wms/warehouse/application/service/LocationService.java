package com.dozycoffee.wms.warehouse.application.service;

import com.dozycoffee.wms.warehouse.application.port.in.GetLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.OccupyLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.ReleaseLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.command.OccupyLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.in.command.ReleaseLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.in.result.LocationResult;
import com.dozycoffee.wms.warehouse.application.port.out.LocationRepository;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.LocationNotFoundException;
import com.dozycoffee.wms.warehouse.domain.model.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class LocationService implements
        RegisterLocationUseCase,
        OccupyLocationUseCase,
        ReleaseLocationUseCase,
        GetLocationUseCase {

    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public Mono<LocationResult> register(RegisterLocationCommand command) {
        Location location = Location.create(
                command.zoneId(),
                command.locationCode(),
                command.maxCapacity(),
                AvailabilityStatus.AVAILABLE
        );
        return locationRepository.save(location)
                .map(LocationResult::from);
    }

    @Override
    @Transactional
    public Mono<LocationResult> occupy(OccupyLocationCommand command) {
        return findLocationOrThrow(command.locationId())
                .doOnNext(location -> location.occupy(command.amount()))
                .flatMap(locationRepository::save)
                .map(LocationResult::from);
    }

    @Override
    @Transactional
    public Mono<LocationResult> release(ReleaseLocationCommand command) {
        return findLocationOrThrow(command.locationId())
                .doOnNext(location -> location.release(command.amount()))
                .flatMap(locationRepository::save)
                .map(LocationResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<LocationResult> getById(Long locationId) {
        return findLocationOrThrow(locationId)
                .map(LocationResult::from);
    }

    private Mono<Location> findLocationOrThrow(Long locationId) {
        return locationRepository.findById(locationId)
                .switchIfEmpty(Mono.error(new LocationNotFoundException()));
    }
}
