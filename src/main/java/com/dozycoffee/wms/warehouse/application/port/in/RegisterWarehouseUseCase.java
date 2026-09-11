package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface RegisterWarehouseUseCase {

    Mono<WarehouseResult> register(RegisterWarehouseCommand command);
}
