package com.dozycoffee.wms.outbound.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface OutboundAllocationR2dbcRepository : CoroutineCrudRepository<OutboundAllocationEntity, Long> {
    fun findAllByOutboundItemIdIn(outboundItemIds: Collection<Long>): Flow<OutboundAllocationEntity>
}
