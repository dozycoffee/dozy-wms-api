package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult

interface ActivateWarehouseUseCase {
    suspend fun activate(warehouseId: Long): WarehouseResult
}
