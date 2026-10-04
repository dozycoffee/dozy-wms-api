package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.inbound.application.port.out.InboundReceiptRepository
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class InboundReceiptPersistenceAdapter(
    private val inboundReceiptR2dbcRepository: InboundReceiptR2dbcRepository
) : InboundReceiptRepository {

    override suspend fun saveAll(receipts: List<InboundReceipt>): List<InboundReceipt> {
        return receipts.map { inboundReceiptR2dbcRepository.save(InboundReceiptEntity.from(it)).toDomain() }
    }

    override suspend fun findAllByInboundItemIds(inboundItemIds: Collection<Long>): List<InboundReceipt> {
        if (inboundItemIds.isEmpty()) return emptyList()
        return inboundReceiptR2dbcRepository.findAllByInboundItemIdIn(inboundItemIds).map { it.toDomain() }.toList()
    }
}
