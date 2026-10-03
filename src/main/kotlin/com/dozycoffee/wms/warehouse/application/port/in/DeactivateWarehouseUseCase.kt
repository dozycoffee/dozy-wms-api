package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult

interface DeactivateWarehouseUseCase {
    suspend fun deactivate(warehouseId: Long): WarehouseResult
}
