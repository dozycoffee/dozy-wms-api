package com.dozycoffee.wms.inbound.fixture

import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.model.Inbound
import java.time.LocalDate

class InboundTestBuilder {

    private var inboundId: Long? = null
    private var warehouseId: Long? = 1L
    private var expectedArrivalDate: LocalDate? = LocalDate.of(2026, 1, 1)
    private var status: InboundStatus = InboundStatus.EXPECTED

    companion object {
        fun inbound(): InboundTestBuilder = InboundTestBuilder()
    }

    fun inboundId(inboundId: Long?): InboundTestBuilder {
        this.inboundId = inboundId
        return this
    }

    fun warehouseId(warehouseId: Long?): InboundTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun expectedArrivalDate(expectedArrivalDate: LocalDate?): InboundTestBuilder {
        this.expectedArrivalDate = expectedArrivalDate
        return this
    }

    fun status(status: InboundStatus): InboundTestBuilder {
        this.status = status
        return this
    }

    fun build(): Inbound {
        val id: Long? = inboundId
        if (id != null) {
            return Inbound.reconstitute(
                inboundId = id,
                warehouseId = requireNotNull(warehouseId) { "warehouseId는 재구성 시 필수입니다." },
                expectedArrivalDate = requireNotNull(expectedArrivalDate) { "expectedArrivalDate는 재구성 시 필수입니다." },
                status = status
            )
        }
        return Inbound.create(
            warehouseId = warehouseId,
            expectedArrivalDate = expectedArrivalDate
        )
    }
}
