package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.result.WarehouseResult;
import reactor.core.publisher.Mono;

public interface GetWarehouseUseCase {

    Mono<WarehouseResult> getById(Long warehouseId);
}
