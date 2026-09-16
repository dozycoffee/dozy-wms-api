package com.dozycoffee.wms.return_request.application.port.out

import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import kotlinx.coroutines.flow.Flow

interface ReturnItemRepository {
    suspend fun save(returnItem: ReturnItem): ReturnItem
    suspend fun findById(returnItemId: Long): ReturnItem?
    fun findAllByReturnRequestId(returnRequestId: Long): Flow<ReturnItem>
}
