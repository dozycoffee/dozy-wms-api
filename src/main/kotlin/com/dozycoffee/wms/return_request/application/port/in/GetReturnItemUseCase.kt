package com.dozycoffee.wms.return_request.application.port.`in`

import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnItemResult
import kotlinx.coroutines.flow.Flow

interface GetReturnItemUseCase {
    fun getAllByReturnRequest(returnRequestId: Long): Flow<ReturnItemResult>
}
