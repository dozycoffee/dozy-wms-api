package com.dozycoffee.wms.return_request.application.port.`in`

import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnRequestResult

interface StartReturnInspectingUseCase {
    suspend fun startInspecting(returnRequestId: Long): ReturnRequestResult
}
