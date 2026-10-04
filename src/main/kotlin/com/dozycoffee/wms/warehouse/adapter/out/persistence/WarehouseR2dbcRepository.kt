package com.dozycoffee.wms.warehouse.adapter.out.persistence

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface WarehouseR2dbcRepository : CoroutineCrudRepository<WarehouseEntity, Long>
