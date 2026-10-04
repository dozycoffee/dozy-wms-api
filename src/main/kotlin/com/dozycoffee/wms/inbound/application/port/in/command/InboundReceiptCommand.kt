package com.dozycoffee.wms.inbound.application.port.`in`.command

import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import java.time.LocalDate

data class InboundReceiptCommand(
    val lotNumber: String,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?,
    val quantity: Int,
    val inspectionResult: InspectionResult,
    val defectReason: DefectReason?
)
