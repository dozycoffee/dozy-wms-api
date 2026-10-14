package com.dozycoffee.wms.outbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.InvalidOutboundStatusTransitionException
import com.dozycoffee.wms.outbound.domain.exception.OutboundErrorCode
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotReallocatableException

class Outbound private constructor(
    val outboundId: Long?,
    val warehouseId: Long,
    /** inventory 예약 ID. 예약 기반으로 전환하기 전의 출고는 null이다 */
    val reservationId: Long?,
    val channel: String?,
    val externalOrderId: String?,
    status: OutboundStatus,
    reallocationCount: Int
) : BaseEntity() {

    /** 재할당 요청을 로컬에 반영한 횟수. 다음 요청의 inventory 멱등 키 회차를 정한다 */
    var reallocationCount: Int = reallocationCount
        private set

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
                status = OutboundStatus.REQUESTED,
                reallocationCount = 0
            )
        }

        fun reconstitute(
            outboundId: Long,
            warehouseId: Long,
            status: OutboundStatus,
            reservationId: Long? = null,
            channel: String? = null,
            externalOrderId: String? = null,
            reallocationCount: Int = 0
        ): Outbound {
            return Outbound(outboundId, warehouseId, reservationId, channel, externalOrderId, status, reallocationCount)
        }
    }

    /** 확정된 예약의 Lot 할당대로 피킹을 시작할 때 호출한다 */
    fun startPicking() = transitionTo(OutboundStatus.PICKING)

    /** 피킹이 끝나 출하 전 검수 단계로 넘어갈 때 호출한다 */
    fun startInspecting() = transitionTo(OutboundStatus.INSPECTING)

    /** 검수를 마치고 출고를 확정할 때 호출한다 */
    fun complete() = transitionTo(OutboundStatus.COMPLETED)

    /** 예약 정보가 있고 피킹 중일 때만 재할당할 수 있다 */
    fun requireReallocatable() {
        if (reservationId == null || status != OutboundStatus.PICKING) {
            throw OutboundNotReallocatableException()
        }
    }

    /** 재할당을 로컬에 반영했을 때 호출한다 */
    fun markReallocated() {
        reallocationCount += 1
    }

    private fun transitionTo(target: OutboundStatus) {
        if (!status.canTransitionTo(target)) {
            throw InvalidOutboundStatusTransitionException()
        }
        status = target
    }
}
