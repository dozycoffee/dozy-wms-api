package com.dozycoffee.wms.return_request.application.port.`in`.command

import java.time.LocalDate

data class ReturnItemLotAssignmentCommand(
    val returnItemId: Long,
    val lotNumber: String,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?
)
