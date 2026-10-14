package com.dozycoffee.wms.disposal.application.port.out

import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import kotlinx.coroutines.flow.Flow

interface DisposalItemRepository {
    suspend fun save(disposalItem: DisposalItem): DisposalItem
    suspend fun findById(disposalItemId: Long): DisposalItem?
    fun findAllByDisposalId(disposalId: Long): Flow<DisposalItem>

    /** 아직 확정되지 않은(완료 전) 폐기 신청이 이 위치 재고에서 폐기하려는 수량의 합 */
    suspend fun sumPendingQuantityByLocationStockId(locationStockId: Long): Int
}
