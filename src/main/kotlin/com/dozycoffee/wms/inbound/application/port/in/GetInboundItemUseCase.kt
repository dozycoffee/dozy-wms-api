package com.dozycoffee.wms.inbound.application.port.`in`

import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundItemResult
import kotlinx.coroutines.flow.Flow

interface GetInboundItemUseCase {
    fun getAllByInbound(inboundId: Long): Flow<InboundItemResult>
}
