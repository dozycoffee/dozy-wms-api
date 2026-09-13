package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.warehouse.application.port.out.WarehouseRepository
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class WarehousePersistenceAdapter(
    private val warehouseR2dbcRepository: WarehouseR2dbcRepository
) : WarehouseRepository {

    override fun save(warehouse: Warehouse): Mono<Warehouse> {
        val entity = WarehouseEntity.from(warehouse)
        val warehouseId = warehouse.warehouseId
            ?: return warehouseR2dbcRepository.save(entity).map { it.toDomain() }
        return warehouseR2dbcRepository.findById(warehouseId)
            .doOnNext { entity.copyAuditFieldsFrom(it) }
            .then(warehouseR2dbcRepository.save(entity))
            .map { it.toDomain() }
    }

    override fun findById(warehouseId: Long): Mono<Warehouse> {
        return warehouseR2dbcRepository.findById(warehouseId).map { it.toDomain() }
    }

    override fun delete(warehouse: Warehouse): Mono<Void> {
        return warehouseR2dbcRepository.delete(WarehouseEntity.from(warehouse))
    }
}
