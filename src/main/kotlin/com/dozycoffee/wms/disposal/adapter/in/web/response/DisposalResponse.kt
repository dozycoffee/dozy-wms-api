package com.dozycoffee.wms.disposal.adapter.`in`.web.response

import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus

data class DisposalResponse(
    val disposalId: Long,
    val warehouseId: Long,
    val status: DisposalStatus
) {
    companion object {
        fun from(result: DisposalResult): DisposalResponse {
            return DisposalResponse(result.disposalId, result.warehouseId, result.status)
        }
    }
}
