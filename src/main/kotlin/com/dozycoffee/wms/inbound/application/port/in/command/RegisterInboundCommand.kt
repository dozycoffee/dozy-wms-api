package com.dozycoffee.wms.inbound.application.port.`in`.command

import java.time.LocalDate

data class RegisterInboundCommand(
    val warehouseId: Long,
    val expectedArrivalDate: LocalDate,
    val items: List<RegisterInboundItemCommand>
)
