package com.dozycoffee.wms.disposal.application.port.`in`.command

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason

data class RegisterDisposalItemCommand(
    val locationStockId: Long,
    val quantity: Int,
    val reason: DisposalReason
)
