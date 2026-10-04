package com.dozycoffee.wms.inbound.adapter.`in`.web.response

import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundReceiptResult
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import java.time.LocalDate

data class InboundReceiptResponse(
    val inboundReceiptId: Long,
    val lotNumber: String,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?,
    val quantity: Int,
    val inspectionResult: InspectionResult,
    val defectReason: DefectReason?
) {
    companion object {
        fun from(result: InboundReceiptResult): InboundReceiptResponse {
            return InboundReceiptResponse(
                result.inboundReceiptId,
                result.lotNumber,
                result.manufactureDate,
                result.expirationDate,
                result.quantity,
                result.inspectionResult,
                result.defectReason
            )
        }
    }
}
