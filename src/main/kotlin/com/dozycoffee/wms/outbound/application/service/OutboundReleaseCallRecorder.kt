package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.outbound.domain.model.Outbound
import org.springframework.stereotype.Component

/**
 * 취소가 확정된 출고의 예약 해제(`POST /reservations/{id}/release`)를 아웃박스에 기록한다 (ADR-0023).
 * inventory의 해제는 예약 ID로 대상을 정하고 상태 기반으로 멱등하므로 본문은 비어 있다.
 */
@Component
class OutboundReleaseCallRecorder(
    private val recordInventoryCallUseCase: RecordInventoryCallUseCase
) {

    suspend fun record(outbound: Outbound) {
        val reservationId: Long = outbound.reservationId ?: return
        val outboundId: Long = requireNotNull(outbound.outboundId)
        recordInventoryCallUseCase.record(
            RecordInventoryCallCommand(
                InventoryCallType.RELEASE,
                outbound.warehouseId,
                "wms-release-$outboundId",
                "/api/v1/reservations/$reservationId/release",
                "{}"
            )
        )
    }
}
