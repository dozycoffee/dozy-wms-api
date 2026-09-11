package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface GetWarehouseUseCase {

    Mono<WarehouseResult> getById(Long warehouseId);
}
