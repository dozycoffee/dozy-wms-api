package com.dozycoffee.wms.warehouse.adapter.out.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface WarehouseR2dbcRepository : ReactiveCrudRepository<WarehouseEntity, Long>
