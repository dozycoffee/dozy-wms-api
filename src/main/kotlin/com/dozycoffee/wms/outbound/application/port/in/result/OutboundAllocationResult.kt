package com.dozycoffee.wms.outbound.application.port.`in`.result

import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import java.time.LocalDate

data class OutboundAllocationResult(
    val outboundAllocationId: Long,
    val outboundItemId: Long,
    val productId: Long,
    val inventoryId: Long,
    val lotNumber: String,
    val expirationDate: LocalDate?,
    val quantity: Int,
    val pickedQuantity: Int?
) {
    companion object {
        fun from(allocation: OutboundAllocation, productId: Long): OutboundAllocationResult {
            return OutboundAllocationResult(
                requireNotNull(allocation.outboundAllocationId),
                allocation.outboundItemId,
                productId,
                allocation.inventoryId,
                allocation.lotNumber,
                allocation.expirationDate,
                allocation.quantity,
                allocation.pickedQuantity
            )
        }
    }
}
