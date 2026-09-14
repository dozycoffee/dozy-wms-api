package com.dozycoffee.wms.inventory.application.port.`in`.result

import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.model.Lot
import java.time.LocalDate

data class LotResult(
    val lotId: Long,
    val lotNumber: String,
    val productId: Long,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?,
    val lotStatus: LotStatus
) {
    companion object {
        fun from(lot: Lot): LotResult {
            return LotResult(
                requireNotNull(lot.lotId),
                lot.lotNumber,
                lot.productId,
                lot.manufactureDate,
                lot.expirationDate,
                lot.lotStatus
            )
        }
    }
}
