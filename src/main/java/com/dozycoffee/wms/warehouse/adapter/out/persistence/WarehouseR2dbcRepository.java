package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface WarehouseR2dbcRepository extends ReactiveCrudRepository<WarehouseEntity, Long> {
}
