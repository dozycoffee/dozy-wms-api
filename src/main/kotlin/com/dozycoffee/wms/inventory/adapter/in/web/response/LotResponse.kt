package com.dozycoffee.wms.inventory.adapter.`in`.web.response

import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import java.time.LocalDate

data class LotResponse(
    val lotId: Long,
    val lotNumber: String,
    val productId: Long,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?,
    val lotStatus: LotStatus
) {
    companion object {
        fun from(result: LotResult): LotResponse {
            return LotResponse(
                result.lotId,
                result.lotNumber,
                result.productId,
                result.manufactureDate,
                result.expirationDate,
                result.lotStatus
            )
        }
    }
}
