package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundAllocationResult

interface GetOutboundAllocationUseCase {
    suspend fun getAllocationsByOutbound(outboundId: Long): List<OutboundAllocationResult>
}
