package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface ReleaseLocationUseCase {

    Mono<LocationResult> release(ReleaseLocationCommand command);
}
