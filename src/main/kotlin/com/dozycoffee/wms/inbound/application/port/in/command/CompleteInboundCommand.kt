package com.dozycoffee.wms.inbound.application.port.`in`.command

data class CompleteInboundCommand(
    val inboundId: Long,
    val lotAssignments: List<LotAssignmentCommand>
)
