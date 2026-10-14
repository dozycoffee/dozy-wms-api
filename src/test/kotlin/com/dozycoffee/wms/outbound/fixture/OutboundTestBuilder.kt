package com.dozycoffee.wms.outbound.fixture

import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.model.Outbound

class OutboundTestBuilder {

    private var outboundId: Long? = null
    private var warehouseId: Long? = 1L
    private var reservationId: Long? = nextReservationId()
    private var channel: String? = "OMS"
    private var externalOrderId: String? = "ORDER-1"
    private var status: OutboundStatus = OutboundStatus.REQUESTED

    companion object {
        private val reservationSequence = java.util.concurrent.atomic.AtomicLong(1000L)

        private fun nextReservationId(): Long = reservationSequence.incrementAndGet()

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

    fun reservationId(reservationId: Long?): OutboundTestBuilder {
        this.reservationId = reservationId
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
                status = status,
                reservationId = reservationId,
                channel = channel,
                externalOrderId = externalOrderId
            )
        }
        return Outbound.createForReservation(warehouseId, reservationId, channel, externalOrderId)
    }
}
