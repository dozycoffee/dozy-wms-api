package com.dozycoffee.wms.disposal.adapter.out.persistence

import com.dozycoffee.wms.disposal.application.port.out.DisposalRepository
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.global.persistence.CommonCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class DisposalPersistenceAdapter(
    private val disposalR2dbcRepository: DisposalR2dbcRepository
) : DisposalRepository {

    companion object {
        private const val STATUS_GROUP = "DISPOSAL_STATUS"
    }

    override suspend fun save(disposal: Disposal): Disposal {
        val entity = DisposalEntity.from(disposal)
        val disposalId = disposal.disposalId
        if (disposalId != null) {
            disposalR2dbcRepository.findById(disposalId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return disposalR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(disposalId: Long): Disposal? {
        return disposalR2dbcRepository.findById(disposalId)?.toDomain()
    }

    override fun findAll(status: DisposalStatus?): Flow<Disposal> {
        val statusCode = status?.let { CommonCodes.toCode(STATUS_GROUP, it) }
        return disposalR2dbcRepository.findAllDisposals(statusCode).map { it.toDomain() }
    }
}
