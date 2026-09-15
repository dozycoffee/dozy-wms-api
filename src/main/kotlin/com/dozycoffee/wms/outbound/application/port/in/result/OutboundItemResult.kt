package com.dozycoffee.wms.outbound.application.port.`in`.result

import com.dozycoffee.wms.outbound.domain.model.OutboundItem

data class OutboundItemResult(
    val outboundItemId: Long,
    val outboundId: Long,
    val productId: Long,
    val requestedQuantity: Int,
    val pickedQuantity: Int?,
    val shortageQuantity: Int?
) {
    companion object {
        fun from(outboundItem: OutboundItem): OutboundItemResult {
            return OutboundItemResult(
                requireNotNull(outboundItem.outboundItemId),
                outboundItem.outboundId,
                outboundItem.productId,
                outboundItem.requestedQuantity,
                outboundItem.pickedQuantity,
                outboundItem.shortageQuantity
            )
        }
    }
}
