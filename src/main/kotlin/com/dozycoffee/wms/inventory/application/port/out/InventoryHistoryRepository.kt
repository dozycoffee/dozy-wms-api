package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface InventoryHistoryRepository {
    suspend fun save(inventoryHistory: InventoryHistory): InventoryHistory

    fun findAll(
        inventoryId: Long?,
        historyType: InventoryHistoryType?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): Flow<InventoryHistory>

    /** 재고 상세 조회에서 최근 이력만 보여주기 위한 조회 — created_at DESC, limit건 */
    fun findRecentByInventoryId(inventoryId: Long, limit: Int): Flow<InventoryHistory>
}
