package com.dozycoffee.wms.inventory.application.port.`in`.command

import java.time.LocalDate

data class RegisterLotCommand(
    val lotNumber: String,
    val productId: Long,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?
)
