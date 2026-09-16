package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryHistoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface GetInventoryHistoryUseCase {
    fun getAll(
        inventoryId: Long?,
        historyType: InventoryHistoryType?,
        from: LocalDate?,
        to: LocalDate?
    ): Flow<InventoryHistoryResult>
}
