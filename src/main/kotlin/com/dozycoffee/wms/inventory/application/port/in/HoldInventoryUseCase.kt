package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.command.HoldInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.AllocationResult

interface HoldInventoryUseCase {
    suspend fun hold(command: HoldInventoryCommand): AllocationResult
}
