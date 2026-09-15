package com.dozycoffee.wms.disposal.adapter.`in`.web.response

import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalItemResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason

data class DisposalItemResponse(
    val disposalItemId: Long,
    val disposalId: Long,
    val inventoryId: Long,
    val quantity: Int,
    val reason: DisposalReason
) {
    companion object {
        fun from(result: DisposalItemResult): DisposalItemResponse {
            return DisposalItemResponse(
                result.disposalItemId,
                result.disposalId,
                result.inventoryId,
                result.quantity,
                result.reason
            )
        }
    }
}
