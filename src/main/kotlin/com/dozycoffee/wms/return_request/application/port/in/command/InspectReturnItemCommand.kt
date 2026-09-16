package com.dozycoffee.wms.return_request.application.port.`in`.command

import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult

data class InspectReturnItemCommand(
    val returnItemId: Long,
    val actualQuantity: Int,
    val inspectionResult: ReturnInspectionResult
)
