package com.dozycoffee.wms.outbound.fixture

import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.model.Outbound

class OutboundTestBuilder {

    private var outboundId: Long? = null
    private var warehouseId: Long? = 1L
    private var status: OutboundStatus = OutboundStatus.REQUESTED

    companion object {
        fun outbound(): OutboundTestBuilder = OutboundTestBuilder()
    }

    fun outboundId(outboundId: Long?): OutboundTestBuilder {
        this.outboundId = outboundId
        return this
    }

    fun warehouseId(warehouseId: Long?): OutboundTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun status(status: OutboundStatus): OutboundTestBuilder {
        this.status = status
        return this
    }

    fun build(): Outbound {
        val id: Long? = outboundId
        if (id != null) {
            return Outbound.reconstitute(
                outboundId = id,
                warehouseId = requireNotNull(warehouseId) { "warehouseId는 재구성 시 필수입니다." },
                status = status
            )
        }
        return Outbound.create(warehouseId = warehouseId)
    }
}
