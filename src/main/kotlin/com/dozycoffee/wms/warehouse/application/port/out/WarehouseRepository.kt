package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import kotlinx.coroutines.flow.Flow

interface WarehouseRepository {
    suspend fun save(warehouse: Warehouse): Warehouse
    suspend fun findById(warehouseId: Long): Warehouse?
    /** [warehouseIds]가 null이면 모든 창고, 아니면 해당 창고만 창고 ID 오름차순으로 조회한다 */
    fun findAll(warehouseIds: List<Long>?): Flow<Warehouse>
    suspend fun delete(warehouse: Warehouse)
}
