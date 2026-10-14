package com.dozycoffee.wms.outbound.adapter.`in`.web.response

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundAllocationResult
import java.time.LocalDate

data class OutboundAllocationResponse(
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
        fun from(result: OutboundAllocationResult): OutboundAllocationResponse {
            return OutboundAllocationResponse(
                result.outboundAllocationId,
                result.outboundItemId,
                result.productId,
                result.inventoryId,
                result.lotNumber,
                result.expirationDate,
                result.quantity,
                result.pickedQuantity
            )
        }
    }
}
