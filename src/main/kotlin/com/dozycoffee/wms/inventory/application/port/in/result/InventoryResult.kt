package com.dozycoffee.wms.inventory.application.port.`in`.result

import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.Inventory

data class InventoryResult(
    val inventoryId: Long,
    val productId: Long,
    val lotId: Long,
    val locationId: Long,
    val quantity: Int,
    val allocatedQuantity: Int,
    val availableQuantity: Int,
    val qualityStatus: QualityStatus
) {
    companion object {
        fun from(inventory: Inventory): InventoryResult {
            return InventoryResult(
                requireNotNull(inventory.inventoryId),
                inventory.productId,
                inventory.lotId,
                inventory.locationId,
                inventory.quantity,
                inventory.allocatedQuantity,
                inventory.availableQuantity,
                inventory.qualityStatus
            )
        }
    }
}
