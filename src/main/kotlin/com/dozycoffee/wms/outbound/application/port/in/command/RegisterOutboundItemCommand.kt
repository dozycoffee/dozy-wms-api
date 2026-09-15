package com.dozycoffee.wms.outbound.application.port.`in`.command

data class RegisterOutboundItemCommand(
    val productId: Long,
    val requestedQuantity: Int
)
