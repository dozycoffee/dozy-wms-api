package com.dozycoffee.wms.outbound.application.port.`in`.result

import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.model.Outbound

data class OutboundResult(
    val outboundId: Long,
    val warehouseId: Long,
    val status: OutboundStatus
) {
    companion object {
        fun from(outbound: Outbound): OutboundResult {
            return OutboundResult(
                requireNotNull(outbound.outboundId),
                outbound.warehouseId,
                outbound.status
            )
        }
    }
}
