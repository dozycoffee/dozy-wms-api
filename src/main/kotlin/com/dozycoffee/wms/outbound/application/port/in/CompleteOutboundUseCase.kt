package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult

interface CompleteOutboundUseCase {
    suspend fun complete(outboundId: Long): OutboundResult
}
