package com.dozycoffee.wms.outbound.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface OutboundItemR2dbcRepository : CoroutineCrudRepository<OutboundItemEntity, Long> {
    fun findAllByOutboundId(outboundId: Long): Flow<OutboundItemEntity>
}
