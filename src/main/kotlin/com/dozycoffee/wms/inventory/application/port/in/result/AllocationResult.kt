package com.dozycoffee.wms.inventory.application.port.`in`.result

import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.model.Allocation

data class AllocationResult(
    val allocationId: Long,
    val inventoryId: Long,
    val referenceType: AllocationReferenceType,
    val referenceId: Long,
    val quantity: Int,
    val status: AllocationStatus
) {
    companion object {
        fun from(allocation: Allocation): AllocationResult {
            return AllocationResult(
                requireNotNull(allocation.allocationId),
                allocation.inventoryId,
                allocation.referenceType,
                allocation.referenceId,
                allocation.quantity,
                allocation.status
            )
        }
    }
}
