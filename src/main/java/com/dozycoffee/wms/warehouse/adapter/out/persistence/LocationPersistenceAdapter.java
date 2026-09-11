package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.warehouse.application.port.out.LocationRepository;
import com.dozycoffee.wms.warehouse.domain.model.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LocationPersistenceAdapter implements LocationRepository {

    private final LocationR2dbcRepository locationR2dbcRepository;

    @Override
    public Mono<Location> save(Location location) {
        LocationEntity entity = LocationEntity.from(location);
        if (location.getLocationId() == null) {
            return locationR2dbcRepository.save(entity)
                    .map(LocationEntity::toDomain);
        }
        return locationR2dbcRepository.findById(location.getLocationId())
                .doOnNext(entity::copyAuditFieldsFrom)
                .then(locationR2dbcRepository.save(entity))
                .map(LocationEntity::toDomain);
    }

    @Override
    public Mono<Location> findById(Long locationId) {
        return locationR2dbcRepository.findById(locationId)
                .map(LocationEntity::toDomain);
    }

    @Override
    public Mono<Void> delete(Location location) {
        return locationR2dbcRepository.delete(LocationEntity.from(location));
    }
}
