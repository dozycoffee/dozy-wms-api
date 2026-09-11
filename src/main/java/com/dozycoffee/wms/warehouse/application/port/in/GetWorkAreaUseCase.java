package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface GetWorkAreaUseCase {

    Mono<WorkAreaResult> getById(Long workAreaId);
}
