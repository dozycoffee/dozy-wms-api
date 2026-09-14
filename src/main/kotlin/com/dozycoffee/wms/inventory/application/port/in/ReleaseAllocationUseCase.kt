package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.AllocationResult

interface ReleaseAllocationUseCase {
    suspend fun release(allocationId: Long): AllocationResult
}
