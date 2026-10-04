package com.dozycoffee.wms.inbound.application.port.`in`.result

import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import java.time.LocalDate

data class InboundItemResult(
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
    val receipts: List<InboundReceiptResult>
) {
    companion object {
        fun from(inboundItem: InboundItem, receipts: List<InboundReceipt> = emptyList()): InboundItemResult {
            return InboundItemResult(
                requireNotNull(inboundItem.inboundItemId),
                inboundItem.inboundId,
                inboundItem.productId,
                inboundItem.zoneId,
                inboundItem.expectedQuantity,
                inboundItem.expectedLotNumber,
                inboundItem.expectedExpirationDate,
                inboundItem.actualQuantity,
                inboundItem.inspectionStatus,
                inboundItem.quantityDiscrepancy,
                receipts.map { InboundReceiptResult.from(it) }
            )
        }
    }
}
