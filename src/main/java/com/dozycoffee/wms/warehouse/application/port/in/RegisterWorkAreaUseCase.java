package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface RegisterWorkAreaUseCase {

    Mono<WorkAreaResult> register(RegisterWorkAreaCommand command);
}
