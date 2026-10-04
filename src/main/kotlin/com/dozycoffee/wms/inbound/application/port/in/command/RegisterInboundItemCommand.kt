package com.dozycoffee.wms.inbound.application.port.`in`.command

import java.time.LocalDate

data class RegisterInboundItemCommand(
    val productId: Long,
    val expectedQuantity: Int,
    val expectedLotNumber: String? = null,
    val expectedExpirationDate: LocalDate? = null
)
