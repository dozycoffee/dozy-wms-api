package com.dozycoffee.wms.disposal.adapter.out.persistence

import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class DisposalItemPersistenceAdapter(
    private val disposalItemR2dbcRepository: DisposalItemR2dbcRepository
) : DisposalItemRepository {

    override suspend fun save(disposalItem: DisposalItem): DisposalItem {
        val entity = DisposalItemEntity.from(disposalItem)
        val disposalItemId = disposalItem.disposalItemId
        if (disposalItemId != null) {
            disposalItemR2dbcRepository.findById(disposalItemId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return disposalItemR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(disposalItemId: Long): DisposalItem? {
        return disposalItemR2dbcRepository.findById(disposalItemId)?.toDomain()
    }

    override fun findAllByDisposalId(disposalId: Long): Flow<DisposalItem> {
        return disposalItemR2dbcRepository.findAllByDisposalId(disposalId).map { it.toDomain() }
    }
}
