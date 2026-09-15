package com.dozycoffee.wms.inbound.application.port.`in`.command

import java.time.LocalDate

data class LotAssignmentCommand(
    val inboundItemId: Long,
    val lotNumber: String,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?
)
