package com.dozycoffee.wms.warehouse_member.application.port.out

import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseAccessEvent
import java.time.LocalDateTime
import java.util.UUID

interface WarehouseAccessEventRepository {
    /**
     * 직원의 발행 대기를 기록한다. 이미 있으면 [eventId]와 [occurredAt]을 새 값으로 바꾸고 시도 정보를 초기화한다.
     * 동시에 호출돼도 한 행으로 합쳐지는 원자적 upsert다.
     */
    suspend fun markChanged(principalId: UUID, eventId: String, occurredAt: LocalDateTime, now: LocalDateTime)

    /** 보낼 차례인 행을 점유(다음 시도 시각을 [leaseUntil]로 미룸)해 가져온다. 트랜잭션 안에서 호출해야 한다 */
    suspend fun claimDue(now: LocalDateTime, leaseUntil: LocalDateTime, limit: Int): List<WarehouseAccessEvent>

    /** 발행에 성공한 행을 지운다. 그 사이 다시 변경되어 [eventId]가 달라졌다면 지우지 않고 false를 돌려준다 */
    suspend fun deleteIfUnchanged(principalId: UUID, eventId: String): Boolean

    /** 발행에 실패한 행의 시도 정보를 갱신한다. [eventId]가 달라졌다면(그 사이 다시 변경) 아무것도 하지 않는다 */
    suspend fun recordFailure(principalId: UUID, eventId: String, attemptCount: Int, lastError: String, nextAttemptAt: LocalDateTime)

    suspend fun count(): Long
}
