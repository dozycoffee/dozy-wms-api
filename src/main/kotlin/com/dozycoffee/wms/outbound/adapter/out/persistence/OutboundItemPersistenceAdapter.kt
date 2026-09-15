package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class OutboundItemPersistenceAdapter(
    private val outboundItemR2dbcRepository: OutboundItemR2dbcRepository
) : OutboundItemRepository {

    override suspend fun save(outboundItem: OutboundItem): OutboundItem {
        val entity = OutboundItemEntity.from(outboundItem)
        val outboundItemId = outboundItem.outboundItemId
        if (outboundItemId != null) {
            outboundItemR2dbcRepository.findById(outboundItemId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return outboundItemR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(outboundItemId: Long): OutboundItem? {
        return outboundItemR2dbcRepository.findById(outboundItemId)?.toDomain()
    }

    override fun findAllByOutboundId(outboundId: Long): Flow<OutboundItem> {
        return outboundItemR2dbcRepository.findAllByOutboundId(outboundId).map { it.toDomain() }
    }
}
