package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult

interface StartOutboundInspectingUseCase {
    suspend fun startInspecting(outboundId: Long): OutboundResult
}
