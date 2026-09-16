package com.dozycoffee.wms.inventory.application.port.`in`.result

import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import java.time.LocalDateTime

data class InventoryHistoryResult(
    val inventoryHistoryId: Long,
    val inventoryId: Long,
    val historyType: InventoryHistoryType,
    val quantityChange: Int,
    val referenceId: Long,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(inventoryHistory: InventoryHistory): InventoryHistoryResult {
            return InventoryHistoryResult(
                requireNotNull(inventoryHistory.inventoryHistoryId),
                inventoryHistory.inventoryId,
                inventoryHistory.historyType,
                inventoryHistory.quantityChange,
                inventoryHistory.referenceId,
                inventoryHistory.createdAt
            )
        }
    }
}
