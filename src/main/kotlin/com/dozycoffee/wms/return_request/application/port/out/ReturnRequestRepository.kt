package com.dozycoffee.wms.return_request.application.port.out

import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import kotlinx.coroutines.flow.Flow

interface ReturnRequestRepository {
    suspend fun save(returnRequest: ReturnRequest): ReturnRequest
    suspend fun findById(returnRequestId: Long): ReturnRequest?
    fun findAll(status: ReturnRequestStatus?): Flow<ReturnRequest>
}
