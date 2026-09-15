package com.dozycoffee.wms.disposal.application.port.`in`.command

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason

data class RegisterDisposalItemCommand(
    val inventoryId: Long,
    val quantity: Int,
    val reason: DisposalReason
)
