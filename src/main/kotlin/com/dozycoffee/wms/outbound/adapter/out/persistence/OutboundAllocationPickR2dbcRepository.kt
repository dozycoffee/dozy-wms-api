package com.dozycoffee.wms.outbound.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface OutboundAllocationPickR2dbcRepository : CoroutineCrudRepository<OutboundAllocationPickEntity, Long> {
    fun findAllByOutboundAllocationIdIn(outboundAllocationIds: Collection<Long>): Flow<OutboundAllocationPickEntity>
    suspend fun deleteAllByOutboundAllocationId(outboundAllocationId: Long)
}
