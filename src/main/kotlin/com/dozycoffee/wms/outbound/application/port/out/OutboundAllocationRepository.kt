package com.dozycoffee.wms.outbound.application.port.out

import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation

interface OutboundAllocationRepository {
    suspend fun save(allocation: OutboundAllocation): OutboundAllocation
    suspend fun delete(allocation: OutboundAllocation)
    suspend fun findAllByOutboundItemIds(outboundItemIds: Collection<Long>): List<OutboundAllocation>
}
