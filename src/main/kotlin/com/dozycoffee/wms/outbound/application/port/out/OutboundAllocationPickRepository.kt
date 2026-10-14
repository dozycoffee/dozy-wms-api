package com.dozycoffee.wms.outbound.application.port.out

import com.dozycoffee.wms.outbound.domain.model.OutboundAllocationPick

interface OutboundAllocationPickRepository {
    suspend fun save(pick: OutboundAllocationPick): OutboundAllocationPick
    suspend fun findAllByOutboundAllocationIds(outboundAllocationIds: Collection<Long>): List<OutboundAllocationPick>
    suspend fun deleteAllByOutboundAllocationId(outboundAllocationId: Long)
}
