package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.model.Warehouse

interface WarehouseRepository {
    suspend fun save(warehouse: Warehouse): Warehouse
    suspend fun findById(warehouseId: Long): Warehouse?
    suspend fun delete(warehouse: Warehouse)
}
