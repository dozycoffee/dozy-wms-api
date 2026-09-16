package com.dozycoffee.wms.return_request.application.port.`in`.result

import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest

data class ReturnRequestResult(
    val returnRequestId: Long,
    val warehouseId: Long,
    val status: ReturnRequestStatus
) {
    companion object {
        fun from(returnRequest: ReturnRequest): ReturnRequestResult {
            return ReturnRequestResult(
                requireNotNull(returnRequest.returnRequestId),
                returnRequest.warehouseId,
                returnRequest.status
            )
        }
    }
}
