package com.dozycoffee.wms.inventory.adapter.`in`.web.response

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryHistoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import java.time.LocalDateTime

data class InventoryHistoryResponse(
    val inventoryHistoryId: Long,
    val inventoryId: Long,
    val historyType: InventoryHistoryType,
    val quantityChange: Int,
    val referenceId: Long,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(result: InventoryHistoryResult): InventoryHistoryResponse {
            return InventoryHistoryResponse(
                result.inventoryHistoryId,
                result.inventoryId,
                result.historyType,
                result.quantityChange,
                result.referenceId,
                result.createdAt
            )
        }
    }
}
