package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.AllocationResult

interface FulfillAllocationUseCase {
    suspend fun fulfill(allocationId: Long): AllocationResult
}
