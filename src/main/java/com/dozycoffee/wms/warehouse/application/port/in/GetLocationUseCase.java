package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface GetLocationUseCase {

    Mono<LocationResult> getById(Long locationId);
}
