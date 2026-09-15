package com.dozycoffee.wms.disposal.application.port.out

import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import kotlinx.coroutines.flow.Flow

interface DisposalItemRepository {
    suspend fun save(disposalItem: DisposalItem): DisposalItem
    suspend fun findById(disposalItemId: Long): DisposalItem?
    fun findAllByDisposalId(disposalId: Long): Flow<DisposalItem>
}
