package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface ReleaseWorkAreaUseCase {

    Mono<WorkAreaResult> release(ReleaseWorkAreaCommand command);
}
