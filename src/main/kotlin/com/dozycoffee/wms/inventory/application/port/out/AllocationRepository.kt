package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.model.Allocation

interface AllocationRepository {
    suspend fun save(allocation: Allocation): Allocation
    suspend fun findById(allocationId: Long): Allocation?

    /** ADR-0008 멱등성 조회 — 같은 참조가 이미 HELD로 점유해둔 Allocation이 있는지 확인한다 */
    suspend fun findHeld(inventoryId: Long, referenceType: AllocationReferenceType, referenceId: Long): Allocation?
}
