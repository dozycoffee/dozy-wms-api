package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import reactor.core.publisher.Mono

interface WarehouseRepository {
    fun save(warehouse: Warehouse): Mono<Warehouse>
    fun findById(warehouseId: Long): Mono<Warehouse>
    fun delete(warehouse: Warehouse): Mono<Void>
}
