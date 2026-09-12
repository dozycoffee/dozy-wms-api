package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.result.WarehouseResult;
import reactor.core.publisher.Mono;

public interface DeactivateWarehouseUseCase {

    Mono<WarehouseResult> deactivate(Long warehouseId);
}
