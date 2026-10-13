package com.dozycoffee.wms.warehouse_member.application.port.out

import java.time.LocalDateTime
import java.util.UUID

interface WarehouseAccessEventPublisher {
    /** `WAREHOUSE_ACCESS_CHANGED`를 보내고 브로커 확인까지 기다린다. 실패하면 예외를 던진다 */
    suspend fun publish(principalId: UUID, eventId: String, occurredAt: LocalDateTime, warehouseIds: List<Long>)
}
