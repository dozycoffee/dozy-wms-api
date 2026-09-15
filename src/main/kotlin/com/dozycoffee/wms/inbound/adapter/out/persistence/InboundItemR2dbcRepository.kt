package com.dozycoffee.wms.inbound.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface InboundItemR2dbcRepository : CoroutineCrudRepository<InboundItemEntity, Long> {
    fun findAllByInboundId(inboundId: Long): Flow<InboundItemEntity>
}
