package com.dozycoffee.wms.outbound.application.port.`in`.command

data class ReportPickingShortageCommand(
    val outboundId: Long,
    val shortages: List<Shortage>
) {
    data class Shortage(val outboundAllocationId: Long, val shortageQuantity: Int)
}
