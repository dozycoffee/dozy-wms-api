package com.dozycoffee.wms.outbound.application.port.out

import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import kotlinx.coroutines.flow.Flow

interface OutboundItemRepository {
    suspend fun save(outboundItem: OutboundItem): OutboundItem
    suspend fun findById(outboundItemId: Long): OutboundItem?
    fun findAllByOutboundId(outboundId: Long): Flow<OutboundItem>
}
