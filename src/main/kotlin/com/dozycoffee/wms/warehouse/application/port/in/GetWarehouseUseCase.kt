package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult

interface GetWarehouseUseCase {
    suspend fun getById(warehouseId: Long): WarehouseResult
}
