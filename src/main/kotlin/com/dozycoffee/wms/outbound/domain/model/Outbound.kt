package com.dozycoffee.wms.outbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.InvalidOutboundStatusTransitionException
import com.dozycoffee.wms.outbound.domain.exception.OutboundErrorCode

class Outbound private constructor(
    val outboundId: Long?,
    val warehouseId: Long,
    status: OutboundStatus
) : BaseEntity() {

    var status: OutboundStatus = status
        private set

    companion object {

        fun create(warehouseId: Long?): Outbound {
            val validWarehouseId: Long = requireNonNull(warehouseId, OutboundErrorCode.INVALID_WAREHOUSE_ID)
            return Outbound(
                outboundId = null,
                warehouseId = validWarehouseId,
                status = OutboundStatus.REQUESTED
            )
        }

        fun reconstitute(outboundId: Long, warehouseId: Long, status: OutboundStatus): Outbound {
            return Outbound(outboundId, warehouseId, status)
        }
    }

    /** FIFO 피킹을 시작할 때 호출한다 */
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
