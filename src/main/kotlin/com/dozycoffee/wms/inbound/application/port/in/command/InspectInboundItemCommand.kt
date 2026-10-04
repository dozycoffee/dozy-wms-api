package com.dozycoffee.wms.inbound.application.port.`in`.command

data class InspectInboundItemCommand(
    val inboundItemId: Long,
    val receipts: List<InboundReceiptCommand>
)
