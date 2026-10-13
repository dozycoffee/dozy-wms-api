package com.dozycoffee.wms.warehouse_member.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * 발행을 기다리는 직원 창고 소속 변경이다. 직원당 한 건만 있고 같은 직원이 다시 바뀌면 [eventId]와 [occurredAt]이 새로 정해진다.
 * 이벤트의 소속 목록은 저장하지 않고 발행 시점의 커밋된 소속을 읽는다(ADR-0020).
 */
class WarehouseAccessEvent(
    val principalId: UUID,
    val eventId: String,
    val occurredAt: LocalDateTime,
    val attemptCount: Int,
    val nextAttemptAt: LocalDateTime,
    val lastError: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WarehouseAccessEvent) return false
        return principalId == other.principalId && eventId == other.eventId
    }

    override fun hashCode(): Int = 31 * principalId.hashCode() + eventId.hashCode()
}
