package com.dozycoffee.wms.inbound.adapter.`in`.web.response

import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundItemResult
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult

data class InboundItemResponse(
    val inboundItemId: Long,
    val inboundId: Long,
    val productId: Long,
    val zoneId: Long,
    val expectedQuantity: Int,
    val actualQuantity: Int?,
    val inspectionResult: InspectionResult,
    val quantityDiscrepancy: Int?
) {
    companion object {
        fun from(result: InboundItemResult): InboundItemResponse {
            return InboundItemResponse(
                result.inboundItemId,
                result.inboundId,
                result.productId,
                result.zoneId,
                result.expectedQuantity,
                result.actualQuantity,
                result.inspectionResult,
                result.quantityDiscrepancy
            )
        }
    }
}
