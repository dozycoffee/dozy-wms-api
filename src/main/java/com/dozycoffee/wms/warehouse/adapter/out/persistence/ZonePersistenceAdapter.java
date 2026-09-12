package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository;
import com.dozycoffee.wms.warehouse.domain.model.Zone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ZonePersistenceAdapter implements ZoneRepository {

    private final ZoneR2dbcRepository zoneR2dbcRepository;

    @Override
    public Mono<Zone> save(Zone zone) {
        ZoneEntity entity = ZoneEntity.from(zone);
        if (zone.getZoneId() == null) {
            return zoneR2dbcRepository.save(entity)
                    .map(ZoneEntity::toDomain);
        }
        return zoneR2dbcRepository.findById(zone.getZoneId())
                .doOnNext(entity::copyAuditFieldsFrom)
                .then(zoneR2dbcRepository.save(entity))
                .map(ZoneEntity::toDomain);
    }

    @Override
    public Mono<Zone> findById(Long zoneId) {
        return zoneR2dbcRepository.findById(zoneId)
                .map(ZoneEntity::toDomain);
    }

    @Override
    public Mono<Void> delete(Zone zone) {
        return zoneR2dbcRepository.delete(ZoneEntity.from(zone));
    }
}
