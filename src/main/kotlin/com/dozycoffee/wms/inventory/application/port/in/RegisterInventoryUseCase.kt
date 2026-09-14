package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult

interface RegisterInventoryUseCase {
    suspend fun register(command: RegisterInventoryCommand): InventoryResult
}
