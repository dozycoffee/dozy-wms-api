package com.dozycoffee.wms.return_request.adapter.`in`.web.response

import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnRequestResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus

data class ReturnRequestResponse(
    val returnRequestId: Long,
    val warehouseId: Long,
    val status: ReturnRequestStatus
) {
    companion object {
        fun from(result: ReturnRequestResult): ReturnRequestResponse {
            return ReturnRequestResponse(result.returnRequestId, result.warehouseId, result.status)
        }
    }
}
