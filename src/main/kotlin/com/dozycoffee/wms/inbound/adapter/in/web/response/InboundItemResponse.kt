package com.dozycoffee.wms.inbound.adapter.`in`.web.response

import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundItemResult
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import java.time.LocalDate

data class InboundItemResponse(
    val inboundItemId: Long,
    val inboundId: Long,
    val productId: Long,
    val zoneId: Long,
    val expectedQuantity: Int,
    val expectedLotNumber: String?,
    val expectedExpirationDate: LocalDate?,
    val actualQuantity: Int?,
    val inspectionStatus: InspectionStatus,
    val quantityDiscrepancy: Int?,
    val receipts: List<InboundReceiptResponse>
) {
    companion object {
        fun from(result: InboundItemResult): InboundItemResponse {
            return InboundItemResponse(
                result.inboundItemId,
                result.inboundId,
                result.productId,
                result.zoneId,
                result.expectedQuantity,
                result.expectedLotNumber,
                result.expectedExpirationDate,
                result.actualQuantity,
                result.inspectionStatus,
                result.quantityDiscrepancy,
                result.receipts.map { InboundReceiptResponse.from(it) }
            )
        }
    }
}
