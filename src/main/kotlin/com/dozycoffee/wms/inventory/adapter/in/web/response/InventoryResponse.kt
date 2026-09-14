package com.dozycoffee.wms.inventory.adapter.`in`.web.response

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus

data class InventoryResponse(
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
        fun from(result: InventoryResult): InventoryResponse {
            return InventoryResponse(
                result.inventoryId,
                result.productId,
                result.lotId,
                result.locationId,
                result.quantity,
                result.allocatedQuantity,
                result.availableQuantity,
                result.qualityStatus
            )
        }
    }
}
