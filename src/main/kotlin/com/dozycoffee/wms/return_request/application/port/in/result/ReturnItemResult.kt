package com.dozycoffee.wms.return_request.application.port.`in`.result

import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.model.ReturnItem

data class ReturnItemResult(
    val returnItemId: Long,
    val returnRequestId: Long,
    val productId: Long,
    val expectedQuantity: Int,
    val actualQuantity: Int?,
    val inspectionResult: ReturnInspectionResult,
    val quantityDiscrepancy: Int?
) {
    companion object {
        fun from(returnItem: ReturnItem): ReturnItemResult {
            return ReturnItemResult(
                requireNotNull(returnItem.returnItemId),
                returnItem.returnRequestId,
                returnItem.productId,
                returnItem.expectedQuantity,
                returnItem.actualQuantity,
                returnItem.inspectionResult,
                returnItem.quantityDiscrepancy
            )
        }
    }
}
