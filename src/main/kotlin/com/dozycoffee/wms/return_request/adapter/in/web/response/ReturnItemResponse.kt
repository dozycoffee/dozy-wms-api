package com.dozycoffee.wms.return_request.adapter.`in`.web.response

import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnItemResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult

data class ReturnItemResponse(
    val returnItemId: Long,
    val returnRequestId: Long,
    val productId: Long,
    val expectedQuantity: Int,
    val actualQuantity: Int?,
    val inspectionResult: ReturnInspectionResult,
    val quantityDiscrepancy: Int?
) {
    companion object {
        fun from(result: ReturnItemResult): ReturnItemResponse {
            return ReturnItemResponse(
                result.returnItemId,
                result.returnRequestId,
                result.productId,
                result.expectedQuantity,
                result.actualQuantity,
                result.inspectionResult,
                result.quantityDiscrepancy
            )
        }
    }
}
