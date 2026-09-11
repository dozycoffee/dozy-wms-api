package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface RegisterLocationUseCase {

    Mono<LocationResult> register(RegisterLocationCommand command);
}
