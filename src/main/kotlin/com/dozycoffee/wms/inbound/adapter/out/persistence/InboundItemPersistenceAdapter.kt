package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class InboundItemPersistenceAdapter(
    private val inboundItemR2dbcRepository: InboundItemR2dbcRepository
) : InboundItemRepository {

    override suspend fun save(inboundItem: InboundItem): InboundItem {
        val entity = InboundItemEntity.from(inboundItem)
        val inboundItemId = inboundItem.inboundItemId
        if (inboundItemId != null) {
            inboundItemR2dbcRepository.findById(inboundItemId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return inboundItemR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(inboundItemId: Long): InboundItem? {
        return inboundItemR2dbcRepository.findById(inboundItemId)?.toDomain()
    }

    override fun findAllByInboundId(inboundId: Long): Flow<InboundItem> {
        return inboundItemR2dbcRepository.findAllByInboundId(inboundId).map { it.toDomain() }
    }
}
