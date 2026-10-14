package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class OutboundAllocationPersistenceAdapter(
    private val outboundAllocationR2dbcRepository: OutboundAllocationR2dbcRepository
) : OutboundAllocationRepository {

    override suspend fun save(allocation: OutboundAllocation): OutboundAllocation {
        val entity = OutboundAllocationEntity.from(allocation)
        val allocationId = allocation.outboundAllocationId
        if (allocationId != null) {
            outboundAllocationR2dbcRepository.findById(allocationId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return outboundAllocationR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun delete(allocation: OutboundAllocation) {
        outboundAllocationR2dbcRepository.deleteById(requireNotNull(allocation.outboundAllocationId))
    }

    override suspend fun findAllByOutboundItemIds(outboundItemIds: Collection<Long>): List<OutboundAllocation> {
        if (outboundItemIds.isEmpty()) return emptyList()
        return outboundAllocationR2dbcRepository.findAllByOutboundItemIdIn(outboundItemIds).map { it.toDomain() }.toList()
    }
}
