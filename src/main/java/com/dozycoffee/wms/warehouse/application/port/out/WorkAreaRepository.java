package com.dozycoffee.wms.warehouse.application.port.out;

import com.dozycoffee.wms.warehouse.domain.model.WorkArea;
import reactor.core.publisher.Mono;

public interface WorkAreaRepository {

    Mono<WorkArea> save(WorkArea workArea);

    Mono<WorkArea> findById(Long workAreaId);

    Mono<Void> delete(WorkArea workArea);
}
