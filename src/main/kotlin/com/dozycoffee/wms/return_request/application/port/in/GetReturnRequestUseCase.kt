package com.dozycoffee.wms.return_request.application.port.`in`

import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnRequestResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import kotlinx.coroutines.flow.Flow

interface GetReturnRequestUseCase {
    suspend fun getById(returnRequestId: Long): ReturnRequestResult
    fun getAll(status: ReturnRequestStatus?): Flow<ReturnRequestResult>
}
