package com.dozycoffee.wms.inbound.application.port.`in`.result

import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.model.Inbound
import java.time.LocalDate

data class InboundResult(
    val inboundId: Long,
    val warehouseId: Long,
    val expectedArrivalDate: LocalDate,
    val status: InboundStatus
) {
    companion object {
        fun from(inbound: Inbound): InboundResult {
            return InboundResult(
                requireNotNull(inbound.inboundId),
                inbound.warehouseId,
                inbound.expectedArrivalDate,
                inbound.status
            )
        }
    }
}
