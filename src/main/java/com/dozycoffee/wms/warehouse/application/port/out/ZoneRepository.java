package com.dozycoffee.wms.warehouse.application.port.out;

import com.dozycoffee.wms.warehouse.domain.model.Zone;
import reactor.core.publisher.Mono;

public interface ZoneRepository {

    Mono<Zone> save(Zone zone);

    Mono<Zone> findById(Long zoneId);

    Mono<Void> delete(Zone zone);
}
