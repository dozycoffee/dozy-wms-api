package com.dozycoffee.wms.disposal.application.port.`in`.command

data class RegisterDisposalCommand(
    val warehouseId: Long,
    val items: List<RegisterDisposalItemCommand>
)
