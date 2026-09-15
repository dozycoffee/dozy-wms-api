package com.dozycoffee.wms.disposal.application.port.`in`.result

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.model.Disposal

data class DisposalResult(
    val disposalId: Long,
    val warehouseId: Long,
    val status: DisposalStatus
) {
    companion object {
        fun from(disposal: Disposal): DisposalResult {
            return DisposalResult(
                requireNotNull(disposal.disposalId),
                disposal.warehouseId,
                disposal.status
            )
        }
    }
}
