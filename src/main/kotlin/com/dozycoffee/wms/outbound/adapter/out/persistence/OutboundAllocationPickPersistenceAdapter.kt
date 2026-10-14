package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationPickRepository
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocationPick
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class OutboundAllocationPickPersistenceAdapter(
    private val outboundAllocationPickR2dbcRepository: OutboundAllocationPickR2dbcRepository
) : OutboundAllocationPickRepository {

    override suspend fun save(pick: OutboundAllocationPick): OutboundAllocationPick {
        return outboundAllocationPickR2dbcRepository.save(OutboundAllocationPickEntity.from(pick)).toDomain()
    }

    override suspend fun findAllByOutboundAllocationIds(outboundAllocationIds: Collection<Long>): List<OutboundAllocationPick> {
        if (outboundAllocationIds.isEmpty()) return emptyList()
        return outboundAllocationPickR2dbcRepository.findAllByOutboundAllocationIdIn(outboundAllocationIds).map { it.toDomain() }.toList()
    }

    override suspend fun deleteAllByOutboundAllocationId(outboundAllocationId: Long) {
        outboundAllocationPickR2dbcRepository.deleteAllByOutboundAllocationId(outboundAllocationId)
    }
}
