package com.dozycoffee.wms.return_request.application.port.`in`.command

data class ReturnItemLotAssignmentCommand(
    val returnItemId: Long,
    val lotNumber: String
)
