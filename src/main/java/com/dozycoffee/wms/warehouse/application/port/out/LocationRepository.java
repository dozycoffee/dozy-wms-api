package com.dozycoffee.wms.warehouse.application.port.out;

import com.dozycoffee.wms.warehouse.domain.model.Location;
import reactor.core.publisher.Mono;

public interface LocationRepository {

    Mono<Location> save(Location location);

    Mono<Location> findById(Long locationId);

    Mono<Void> delete(Location location);
}
