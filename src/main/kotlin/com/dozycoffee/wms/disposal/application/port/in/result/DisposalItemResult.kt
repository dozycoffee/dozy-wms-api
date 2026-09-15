package com.dozycoffee.wms.disposal.application.port.`in`.result

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.model.DisposalItem

data class DisposalItemResult(
    val disposalItemId: Long,
    val disposalId: Long,
    val inventoryId: Long,
    val quantity: Int,
    val reason: DisposalReason
) {
    companion object {
        fun from(disposalItem: DisposalItem): DisposalItemResult {
            return DisposalItemResult(
                requireNotNull(disposalItem.disposalItemId),
                disposalItem.disposalId,
                disposalItem.inventoryId,
                disposalItem.quantity,
                disposalItem.reason
            )
        }
    }
}
