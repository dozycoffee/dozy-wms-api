package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.result.LocationResult;
import reactor.core.publisher.Mono;

public interface GetLocationUseCase {

    Mono<LocationResult> getById(Long locationId);
}
