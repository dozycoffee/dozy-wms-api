package com.dozycoffee.wms.return_request.adapter.out.persistence

import com.dozycoffee.wms.return_request.application.port.out.ReturnItemRepository
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class ReturnItemPersistenceAdapter(
    private val returnItemR2dbcRepository: ReturnItemR2dbcRepository
) : ReturnItemRepository {

    override suspend fun save(returnItem: ReturnItem): ReturnItem {
        val entity = ReturnItemEntity.from(returnItem)
        val returnItemId = returnItem.returnItemId
        if (returnItemId != null) {
            returnItemR2dbcRepository.findById(returnItemId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return returnItemR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(returnItemId: Long): ReturnItem? {
        return returnItemR2dbcRepository.findById(returnItemId)?.toDomain()
    }

    override fun findAllByReturnRequestId(returnRequestId: Long): Flow<ReturnItem> {
        return returnItemR2dbcRepository.findAllByReturnRequestId(returnRequestId).map { it.toDomain() }
    }
}
