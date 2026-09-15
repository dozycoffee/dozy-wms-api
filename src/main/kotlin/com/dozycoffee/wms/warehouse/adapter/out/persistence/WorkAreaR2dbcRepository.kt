package com.dozycoffee.wms.warehouse.adapter.out.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface WorkAreaR2dbcRepository : ReactiveCrudRepository<WorkAreaEntity, Long> {
    fun findByWarehouseIdAndAreaCode(warehouseId: Long, areaCode: String): Mono<WorkAreaEntity>
}
