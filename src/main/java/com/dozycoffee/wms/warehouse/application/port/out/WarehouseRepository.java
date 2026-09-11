package com.dozycoffee.wms.warehouse.application.port.out;

import com.dozycoffee.wms.warehouse.domain.model.Warehouse;
import reactor.core.publisher.Mono;

public interface WarehouseRepository {

    Mono<Warehouse> save(Warehouse warehouse);

    Mono<Warehouse> findById(Long warehouseId);

    Mono<Void> delete(Warehouse warehouse);
}
