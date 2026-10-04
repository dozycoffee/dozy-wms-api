package com.dozycoffee.wms.inbound.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface InboundReceiptR2dbcRepository : CoroutineCrudRepository<InboundReceiptEntity, Long> {
    fun findAllByInboundItemIdIn(inboundItemIds: Collection<Long>): Flow<InboundReceiptEntity>
}
