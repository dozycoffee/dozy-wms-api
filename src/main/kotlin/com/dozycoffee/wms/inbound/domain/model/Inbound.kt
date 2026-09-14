package com.dozycoffee.wms.inbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.exception.InboundErrorCode
import com.dozycoffee.wms.inbound.domain.exception.InvalidInboundStatusTransitionException
import java.time.LocalDate

class Inbound private constructor(
    val inboundId: Long?,
    val warehouseId: Long,
    val expectedArrivalDate: LocalDate,
    status: InboundStatus
) : BaseEntity() {

    var status: InboundStatus = status
        private set

    companion object {

        fun create(warehouseId: Long?, expectedArrivalDate: LocalDate?): Inbound {
            val validWarehouseId: Long = requireNonNull(warehouseId, InboundErrorCode.INVALID_WAREHOUSE_ID)
            val validExpectedArrivalDate: LocalDate =
                requireNonNull(expectedArrivalDate, InboundErrorCode.INVALID_EXPECTED_ARRIVAL_DATE)
            return Inbound(
                inboundId = null,
                warehouseId = validWarehouseId,
                expectedArrivalDate = validExpectedArrivalDate,
                status = InboundStatus.EXPECTED
            )
        }

        fun reconstitute(
            inboundId: Long,
            warehouseId: Long,
            expectedArrivalDate: LocalDate,
            status: InboundStatus
        ): Inbound {
            return Inbound(inboundId, warehouseId, expectedArrivalDate, status)
        }
    }

    /** 목적지 Zone(들)의 capacity 사전 점검을 통과해 상품 도착 대기 상태로 전환할 때 호출한다 (ADR-0003) */
    fun markWaiting() = transitionTo(InboundStatus.WAITING)

    /** 상품이 도착해 입고 처리장으로 이동, 검수를 시작할 때 호출한다 */
    fun startProcessing() = transitionTo(InboundStatus.PROCESSING)

    /** 검수를 마친 상품의 적재까지 모두 끝났을 때 호출한다 */
    fun complete() = transitionTo(InboundStatus.COMPLETED)

    private fun transitionTo(target: InboundStatus) {
        if (!status.canTransitionTo(target)) {
            throw InvalidInboundStatusTransitionException()
        }
        status = target
    }
}
