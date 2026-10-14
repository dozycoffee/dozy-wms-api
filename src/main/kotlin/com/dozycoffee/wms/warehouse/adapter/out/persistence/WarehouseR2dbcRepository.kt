package com.dozycoffee.wms.warehouse.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface WarehouseR2dbcRepository : CoroutineCrudRepository<WarehouseEntity, Long> {

    @Query("SELECT * FROM warehouse WHERE deleted_at IS NULL ORDER BY warehouse_id")
    fun findAllActive(): Flow<WarehouseEntity>

    @Query("SELECT * FROM warehouse WHERE deleted_at IS NULL AND warehouse_id IN (:warehouseIds) ORDER BY warehouse_id")
    fun findAllActiveInWarehouses(warehouseIds: List<Long>): Flow<WarehouseEntity>
}
