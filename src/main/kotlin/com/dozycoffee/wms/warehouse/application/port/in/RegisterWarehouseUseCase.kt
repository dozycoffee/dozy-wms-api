package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWarehouseCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult

interface RegisterWarehouseUseCase {
    suspend fun register(command: RegisterWarehouseCommand): WarehouseResult
}
