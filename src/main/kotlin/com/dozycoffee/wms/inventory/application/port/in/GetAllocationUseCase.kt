package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.AllocationResult
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import kotlinx.coroutines.flow.Flow

interface GetAllocationUseCase {
    fun getAllHeldByReference(referenceType: AllocationReferenceType, referenceId: Long): Flow<AllocationResult>
}
