package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface ActivateWarehouseUseCase {

    Mono<WarehouseResult> activate(Long warehouseId);
}
