package com.dozycoffee.wms.inbound.application.port.`in`.command

import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult

data class InspectInboundItemCommand(
    val inboundItemId: Long,
    val actualQuantity: Int,
    val inspectionResult: InspectionResult
)
