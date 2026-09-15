package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.model.Inbound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class InboundPersistenceAdapter(
    private val inboundR2dbcRepository: InboundR2dbcRepository
) : InboundRepository {

    companion object {
        private const val STATUS_GROUP = "INBOUND_STATUS"
    }

    override suspend fun save(inbound: Inbound): Inbound {
        val entity = InboundEntity.from(inbound)
        val inboundId = inbound.inboundId
        if (inboundId != null) {
            inboundR2dbcRepository.findById(inboundId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return inboundR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(inboundId: Long): Inbound? {
        return inboundR2dbcRepository.findById(inboundId)?.toDomain()
    }

    override fun findAll(status: InboundStatus?): Flow<Inbound> {
        val statusCode = status?.let { CommonCodes.toCode(STATUS_GROUP, it) }
        return inboundR2dbcRepository.findAllInbounds(statusCode).map { it.toDomain() }
    }
}
