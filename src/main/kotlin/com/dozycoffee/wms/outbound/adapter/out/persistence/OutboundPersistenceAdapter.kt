package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.model.Outbound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class OutboundPersistenceAdapter(
    private val outboundR2dbcRepository: OutboundR2dbcRepository
) : OutboundRepository {

    companion object {
        private const val STATUS_GROUP = "OUTBOUND_STATUS"
    }

    override suspend fun save(outbound: Outbound): Outbound {
        val entity = OutboundEntity.from(outbound)
        val outboundId = outbound.outboundId
        if (outboundId != null) {
            outboundR2dbcRepository.findById(outboundId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return outboundR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(outboundId: Long): Outbound? {
        return outboundR2dbcRepository.findById(outboundId)?.toDomain()
    }

    override fun findAll(status: OutboundStatus?): Flow<Outbound> {
        val statusCode = status?.let { CommonCodes.toCode(STATUS_GROUP, it) }
        return outboundR2dbcRepository.findAllOutbounds(statusCode).map { it.toDomain() }
    }
}
