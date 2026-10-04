package com.dozycoffee.wms.inbound.application.port.`in`.result

import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import java.time.LocalDate

data class InboundReceiptResult(
    val inboundReceiptId: Long,
    val lotNumber: String,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?,
    val quantity: Int,
    val inspectionResult: InspectionResult,
    val defectReason: DefectReason?
) {
    companion object {
        fun from(receipt: InboundReceipt): InboundReceiptResult {
            return InboundReceiptResult(
                requireNotNull(receipt.inboundReceiptId),
                receipt.lotNumber,
                receipt.manufactureDate,
                receipt.expirationDate,
                receipt.quantity,
                receipt.inspectionResult,
                receipt.defectReason
            )
        }
    }
}
