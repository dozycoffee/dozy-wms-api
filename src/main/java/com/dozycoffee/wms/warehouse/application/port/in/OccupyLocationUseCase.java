package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface OccupyLocationUseCase {

    Mono<LocationResult> occupy(OccupyLocationCommand command);
}
