package com.dozycoffee.wms.inbound.application.port.`in`.result

import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.model.InboundItem

data class InboundItemResult(
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
        fun from(inboundItem: InboundItem): InboundItemResult {
            return InboundItemResult(
                requireNotNull(inboundItem.inboundItemId),
                inboundItem.inboundId,
                inboundItem.productId,
                inboundItem.zoneId,
                inboundItem.expectedQuantity,
                inboundItem.actualQuantity,
                inboundItem.inspectionResult,
                inboundItem.quantityDiscrepancy
            )
        }
    }
}
