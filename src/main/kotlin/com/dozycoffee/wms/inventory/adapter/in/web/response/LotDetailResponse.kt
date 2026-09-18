package com.dozycoffee.wms.inventory.adapter.`in`.web.response

import com.dozycoffee.wms.inventory.application.port.`in`.result.LotDetailResult

data class LotDetailResponse(
    val lot: LotResponse,
    val distribution: List<LotDistributionResponse>
) {
    companion object {
        fun from(result: LotDetailResult): LotDetailResponse {
            return LotDetailResponse(
                LotResponse.from(result.lot),
                result.distribution.map { LotDistributionResponse.from(it) }
            )
        }
    }
}
