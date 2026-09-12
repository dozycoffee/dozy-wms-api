package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.result.WorkAreaResult;
import reactor.core.publisher.Mono;

public interface GetWorkAreaUseCase {

    Mono<WorkAreaResult> getById(Long workAreaId);
}
