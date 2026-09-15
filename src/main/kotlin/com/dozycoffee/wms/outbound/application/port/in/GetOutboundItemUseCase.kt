package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundItemResult
import kotlinx.coroutines.flow.Flow

interface GetOutboundItemUseCase {
    fun getAllByOutbound(outboundId: Long): Flow<OutboundItemResult>
}
