package com.dozycoffee.wms.inbound.application.port.`in`

import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundResult

interface StartInboundProcessingUseCase {
    suspend fun startProcessing(inboundId: Long): InboundResult
}
