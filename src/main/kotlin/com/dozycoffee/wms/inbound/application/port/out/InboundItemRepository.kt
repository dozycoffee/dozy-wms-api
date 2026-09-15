package com.dozycoffee.wms.inbound.application.port.out

import com.dozycoffee.wms.inbound.domain.model.InboundItem
import kotlinx.coroutines.flow.Flow

interface InboundItemRepository {
    suspend fun save(inboundItem: InboundItem): InboundItem
    suspend fun findById(inboundItemId: Long): InboundItem?
    fun findAllByInboundId(inboundId: Long): Flow<InboundItem>
}
