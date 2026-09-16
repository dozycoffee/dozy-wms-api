package com.dozycoffee.wms.return_request.application.port.`in`.command

data class RegisterReturnItemCommand(
    val productId: Long,
    val expectedQuantity: Int
)
