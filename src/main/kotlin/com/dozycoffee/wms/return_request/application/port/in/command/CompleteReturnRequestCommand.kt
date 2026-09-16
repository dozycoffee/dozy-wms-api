package com.dozycoffee.wms.return_request.application.port.`in`.command

data class CompleteReturnRequestCommand(
    val returnRequestId: Long,
    val lotAssignments: List<ReturnItemLotAssignmentCommand>
)
