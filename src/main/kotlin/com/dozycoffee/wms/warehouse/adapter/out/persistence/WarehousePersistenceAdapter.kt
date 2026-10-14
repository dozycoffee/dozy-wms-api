package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.warehouse.application.port.out.WarehouseRepository
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class WarehousePersistenceAdapter(
    private val warehouseR2dbcRepository: WarehouseR2dbcRepository
) : WarehouseRepository {

    override suspend fun save(warehouse: Warehouse): Warehouse {
        val entity = WarehouseEntity.from(warehouse)
        val warehouseId = warehouse.warehouseId
        if (warehouseId != null) {
            warehouseR2dbcRepository.findById(warehouseId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return warehouseR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(warehouseId: Long): Warehouse? {
        return warehouseR2dbcRepository.findById(warehouseId)?.toDomain()
    }

    override fun findAll(warehouseIds: List<Long>?): Flow<Warehouse> {
        val ids: List<Long>? = warehouseIds?.takeIf { it.isNotEmpty() }
        val entities: Flow<WarehouseEntity> =
            if (ids == null) warehouseR2dbcRepository.findAllActive() else warehouseR2dbcRepository.findAllActiveInWarehouses(ids)
        return entities.map { it.toDomain() }
    }

    override suspend fun delete(warehouse: Warehouse) {
        warehouseR2dbcRepository.delete(WarehouseEntity.from(warehouse))
    }
}
