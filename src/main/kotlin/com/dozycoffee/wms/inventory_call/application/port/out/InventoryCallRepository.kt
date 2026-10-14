package com.dozycoffee.wms.inventory_call.application.port.out

import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface InventoryCallRepository {
    suspend fun save(inventoryCall: InventoryCall): InventoryCall

    suspend fun findById(inventoryCallId: Long): InventoryCall?

    suspend fun findByIdempotencyKey(idempotencyKey: String): InventoryCall?

    /** 창고에서 아직 끝나지 않은(PENDING, DEAD) 호출 수 */
    suspend fun countUnfinishedByWarehouseId(warehouseId: Long): Long

    fun findAll(status: InventoryCallStatus?, limit: Int): Flow<InventoryCall>

    /**
     * 창고별로 보낼 차례인 호출을 잠그고 가져온다. 같은 창고에 자신보다 앞선 PENDING/DEAD가 없고 [now]에 이미 도래한 호출만이며
     * 다른 트랜잭션이 잠근 행은 건너뛴다. 트랜잭션 안에서 호출해야 한다.
     */
    fun findDispatchable(now: LocalDateTime, limit: Int): Flow<InventoryCall>
}
