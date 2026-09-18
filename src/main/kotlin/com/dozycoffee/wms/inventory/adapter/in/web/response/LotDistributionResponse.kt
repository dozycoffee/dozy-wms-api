package com.dozycoffee.wms.inventory.adapter.`in`.web.response

import com.dozycoffee.wms.inventory.application.port.`in`.result.LotDistributionResult
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

data class LotDistributionResponse(
    val locationId: Long,
    val zoneId: Long,
    val zoneCode: ZoneCode,
    val quantity: Int
) {
    companion object {
        fun from(result: LotDistributionResult): LotDistributionResponse {
            return LotDistributionResponse(result.locationId, result.zoneId, result.zoneCode, result.quantity)
        }
    }
}
