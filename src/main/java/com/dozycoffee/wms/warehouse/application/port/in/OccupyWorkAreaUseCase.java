package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface OccupyWorkAreaUseCase {

    Mono<WorkAreaResult> occupy(OccupyWorkAreaCommand command);
}
