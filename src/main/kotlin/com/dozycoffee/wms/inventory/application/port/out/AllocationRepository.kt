package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.model.Allocation
import kotlinx.coroutines.flow.Flow

interface AllocationRepository {
    suspend fun save(allocation: Allocation): Allocation
    suspend fun findById(allocationId: Long): Allocation?

    /** ADR-0008 멱등성 조회 — 같은 참조가 이미 HELD로 점유해둔 Allocation이 있는지 확인한다 */
    suspend fun findHeld(inventoryId: Long, referenceType: AllocationReferenceType, referenceId: Long): Allocation?

    /** 점유 주체(예: 출고 상품) 기준으로 아직 HELD 상태인 점유 전체를 조회한다 — 여러 Inventory에 걸쳐 분할 점유될 수 있다 */
    fun findAllHeldByReference(referenceType: AllocationReferenceType, referenceId: Long): Flow<Allocation>
}
