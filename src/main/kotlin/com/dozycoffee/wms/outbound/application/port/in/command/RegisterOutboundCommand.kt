package com.dozycoffee.wms.outbound.application.port.`in`.command

data class RegisterOutboundCommand(
    val warehouseId: Long,
    val items: List<RegisterOutboundItemCommand>
)
