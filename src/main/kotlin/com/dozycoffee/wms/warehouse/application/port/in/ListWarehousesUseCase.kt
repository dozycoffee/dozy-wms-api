package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult
import kotlinx.coroutines.flow.Flow

interface ListWarehousesUseCase {
    fun getAll(): Flow<WarehouseResult>
}
