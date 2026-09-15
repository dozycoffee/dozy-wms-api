package com.dozycoffee.wms.outbound.adapter.`in`.web.response

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundItemResult

data class OutboundItemResponse(
    val outboundItemId: Long,
    val outboundId: Long,
    val productId: Long,
    val requestedQuantity: Int,
    val pickedQuantity: Int?,
    val shortageQuantity: Int?
) {
    companion object {
        fun from(result: OutboundItemResult): OutboundItemResponse {
            return OutboundItemResponse(
                result.outboundItemId,
                result.outboundId,
                result.productId,
                result.requestedQuantity,
                result.pickedQuantity,
                result.shortageQuantity
            )
        }
    }
}
