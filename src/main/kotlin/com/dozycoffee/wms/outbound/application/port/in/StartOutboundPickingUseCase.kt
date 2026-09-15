package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult

interface StartOutboundPickingUseCase {
    suspend fun startPicking(outboundId: Long): OutboundResult
}
