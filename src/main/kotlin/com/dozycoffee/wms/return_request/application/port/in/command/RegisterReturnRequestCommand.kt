package com.dozycoffee.wms.return_request.application.port.`in`.command

data class RegisterReturnRequestCommand(
    val warehouseId: Long,
    val items: List<RegisterReturnItemCommand>
)
