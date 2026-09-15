package com.dozycoffee.wms.inbound.application.port.`in`.command

data class RegisterInboundItemCommand(
    val productId: Long,
    val expectedQuantity: Int
)
