package com.dozycoffee.wms.disposal.adapter.`in`.web.response

import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalItemResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason

data class DisposalItemResponse(
    val disposalItemId: Long,
    val disposalId: Long,
    val locationStockId: Long,
    val quantity: Int,
    val reason: DisposalReason
) {
    companion object {
        fun from(result: DisposalItemResult): DisposalItemResponse {
            return DisposalItemResponse(
                result.disposalItemId,
                result.disposalId,
                result.locationStockId,
                result.quantity,
                result.reason
            )
        }
    }
}
