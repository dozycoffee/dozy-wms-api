package com.dozycoffee.wms.outbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.InvalidOutboundStatusTransitionException
import com.dozycoffee.wms.outbound.domain.exception.OutboundErrorCode

class Outbound private constructor(
    val outboundId: Long?,
    val warehouseId: Long,
    /** inventory 예약 ID. 예약 기반으로 전환하기 전의 출고는 null이다 */
    val reservationId: Long?,
    val channel: String?,
    val externalOrderId: String?,
    status: OutboundStatus
) : BaseEntity() {

    var status: OutboundStatus = status
        private set

    companion object {

        fun createForReservation(warehouseId: Long?, reservationId: Long?, channel: String?, externalOrderId: String?): Outbound {
            val validWarehouseId: Long = requireNonNull(warehouseId, OutboundErrorCode.INVALID_WAREHOUSE_ID)
            val validReservationId: Long = requireNonNull(reservationId, OutboundErrorCode.INVALID_RESERVATION_ID)
            return Outbound(
                outboundId = null,
                warehouseId = validWarehouseId,
                reservationId = validReservationId,
                channel = channel,
                externalOrderId = externalOrderId,
                status = OutboundStatus.REQUESTED
            )
        }

        fun reconstitute(
            outboundId: Long,
            warehouseId: Long,
            status: OutboundStatus,
            reservationId: Long? = null,
            channel: String? = null,
            externalOrderId: String? = null
        ): Outbound {
            return Outbound(outboundId, warehouseId, reservationId, channel, externalOrderId, status)
        }
    }

    /** 확정된 예약의 Lot 할당대로 피킹을 시작할 때 호출한다 */
    fun startPicking() = transitionTo(OutboundStatus.PICKING)

    /** 피킹이 끝나 출하 전 검수 단계로 넘어갈 때 호출한다 */
    fun startInspecting() = transitionTo(OutboundStatus.INSPECTING)

    /** 검수를 마치고 출고를 확정할 때 호출한다 */
    fun complete() = transitionTo(OutboundStatus.COMPLETED)

    private fun transitionTo(target: OutboundStatus) {
        if (!status.canTransitionTo(target)) {
            throw InvalidOutboundStatusTransitionException()
        }
        status = target
    }
}
