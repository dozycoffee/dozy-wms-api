package com.dozycoffee.wms.return_request.application.port.`in`

import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnRequestResult

interface CompleteReturnRequestUseCase {
    suspend fun complete(returnRequestId: Long): ReturnRequestResult
}
