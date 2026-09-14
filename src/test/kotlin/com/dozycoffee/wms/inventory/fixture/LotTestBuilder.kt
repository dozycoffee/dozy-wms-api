package com.dozycoffee.wms.inventory.fixture

import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.model.Lot
import java.time.LocalDate

class LotTestBuilder {

    private var lotId: Long? = null
    private var lotNumber: String? = "LOT-20260101-001"
    private var productId: Long? = 1L
    private var manufactureDate: LocalDate? = LocalDate.of(2026, 1, 1)
    private var expirationDate: LocalDate? = LocalDate.of(2026, 12, 31)
    private var lotStatus: LotStatus = LotStatus.NORMAL

    companion object {
        fun lot(): LotTestBuilder = LotTestBuilder()
    }

    fun lotId(lotId: Long?): LotTestBuilder {
        this.lotId = lotId
        return this
    }

    fun lotNumber(lotNumber: String?): LotTestBuilder {
        this.lotNumber = lotNumber
        return this
    }

    fun productId(productId: Long?): LotTestBuilder {
        this.productId = productId
        return this
    }

    fun manufactureDate(manufactureDate: LocalDate?): LotTestBuilder {
        this.manufactureDate = manufactureDate
        return this
    }

    fun expirationDate(expirationDate: LocalDate?): LotTestBuilder {
        this.expirationDate = expirationDate
        return this
    }

    fun lotStatus(lotStatus: LotStatus): LotTestBuilder {
        this.lotStatus = lotStatus
        return this
    }

    fun build(): Lot {
        val id: Long? = lotId
        if (id != null) {
            return Lot.reconstitute(
                lotId = id,
                lotNumber = requireNotNull(lotNumber) { "lotNumber는 재구성 시 필수입니다." },
                productId = requireNotNull(productId) { "productId는 재구성 시 필수입니다." },
                manufactureDate = manufactureDate,
                expirationDate = expirationDate,
                lotStatus = lotStatus
            )
        }
        return Lot.create(
            lotNumber = lotNumber,
            productId = productId,
            manufactureDate = manufactureDate,
            expirationDate = expirationDate
        )
    }
}
