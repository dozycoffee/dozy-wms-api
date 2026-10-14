package com.dozycoffee.wms.outbound.adapter.`in`.web.response

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus

data class OutboundResponse(
    val outboundId: Long,
    val warehouseId: Long,
    val status: OutboundStatus,
    val reservationId: Long?,
    val channel: String?,
    val externalOrderId: String?
) {
    companion object {
        fun from(result: OutboundResult): OutboundResponse {
            return OutboundResponse(
                result.outboundId,
                result.warehouseId,
                result.status,
                result.reservationId,
                result.channel,
                result.externalOrderId
            )
        }
    }
}
