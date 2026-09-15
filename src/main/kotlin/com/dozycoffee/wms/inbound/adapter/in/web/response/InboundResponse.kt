package com.dozycoffee.wms.inbound.adapter.`in`.web.response

import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundResult
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import java.time.LocalDate

data class InboundResponse(
    val inboundId: Long,
    val warehouseId: Long,
    val expectedArrivalDate: LocalDate,
    val status: InboundStatus
) {
    companion object {
        fun from(result: InboundResult): InboundResponse {
            return InboundResponse(
                result.inboundId,
                result.warehouseId,
                result.expectedArrivalDate,
                result.status
            )
        }
    }
}
