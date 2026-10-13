package com.dozycoffee.wms.warehouse_member.adapter.out.persistence

import com.dozycoffee.wms.global.security.CurrentActorProvider
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventRepository
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseAccessEvent
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

/**
 * 직원 한 명당 한 행이라 원자적 upsert와 조건부 갱신이 필요해 [DatabaseClient]로 직접 SQL을 쓴다. 감사 컬럼도 직접 채운다.
 * 점유는 `FOR UPDATE SKIP LOCKED`로 고르고 다음 시도 시각을 미루는 방식(lease)이라 Kafka 전송 중에는 행 잠금을 잡지 않는다.
 */
@Component
class WarehouseAccessEventPersistenceAdapter(
    private val databaseClient: DatabaseClient,
    private val currentActorProvider: CurrentActorProvider
) : WarehouseAccessEventRepository {

    override suspend fun markChanged(principalId: UUID, eventId: String, occurredAt: LocalDateTime, now: LocalDateTime) {
        val actor: String = currentActorProvider.get().auditName
        databaseClient.sql(
            """
            INSERT INTO warehouse_access_event
                (principal_id, event_id, occurred_at, attempt_count, next_attempt_at, last_error,
                 created_at, created_by, updated_at, updated_by)
            VALUES (:principalId, :eventId, :occurredAt, 0, :now, NULL, :now, :actor, :now, :actor)
            ON DUPLICATE KEY UPDATE
                event_id = VALUES(event_id), occurred_at = VALUES(occurred_at), attempt_count = 0,
                next_attempt_at = VALUES(next_attempt_at), last_error = NULL,
                updated_at = VALUES(updated_at), updated_by = VALUES(updated_by)
            """.trimIndent()
        )
            .bind("principalId", principalId.toString())
            .bind("eventId", eventId)
            .bind("occurredAt", occurredAt)
            .bind("now", now)
            .bind("actor", actor)
            .fetch()
            .awaitRowsUpdated()
    }

    override suspend fun claimDue(now: LocalDateTime, leaseUntil: LocalDateTime, limit: Int): List<WarehouseAccessEvent> {
        val due: List<WarehouseAccessEvent> = databaseClient.sql(
            """
            SELECT principal_id, event_id, occurred_at, attempt_count, next_attempt_at, last_error
            FROM warehouse_access_event
            WHERE next_attempt_at <= :now
            ORDER BY warehouse_access_event_id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """.trimIndent()
        )
            .bind("now", now)
            .bind("limit", limit)
            .map { row, _ ->
                WarehouseAccessEvent(
                    principalId = UUID.fromString(requireNotNull(row.get("principal_id", String::class.java))),
                    eventId = requireNotNull(row.get("event_id", String::class.java)),
                    occurredAt = requireNotNull(row.get("occurred_at", LocalDateTime::class.java)),
                    attemptCount = requireNotNull(row.get("attempt_count", Number::class.java)).toInt(),
                    nextAttemptAt = requireNotNull(row.get("next_attempt_at", LocalDateTime::class.java)),
                    lastError = row.get("last_error", String::class.java)
                )
            }
            .flow()
            .toList()
        for (event in due) {
            databaseClient.sql(
                "UPDATE warehouse_access_event SET next_attempt_at = :leaseUntil WHERE principal_id = :principalId AND event_id = :eventId"
            )
                .bind("leaseUntil", leaseUntil)
                .bind("principalId", event.principalId.toString())
                .bind("eventId", event.eventId)
                .fetch()
                .awaitRowsUpdated()
        }
        return due
    }

    override suspend fun deleteIfUnchanged(principalId: UUID, eventId: String): Boolean {
        val deleted: Long = databaseClient.sql(
            "DELETE FROM warehouse_access_event WHERE principal_id = :principalId AND event_id = :eventId"
        )
            .bind("principalId", principalId.toString())
            .bind("eventId", eventId)
            .fetch()
            .awaitRowsUpdated()
        return deleted > 0
    }

    override suspend fun recordFailure(
        principalId: UUID,
        eventId: String,
        attemptCount: Int,
        lastError: String,
        nextAttemptAt: LocalDateTime
    ) {
        databaseClient.sql(
            """
            UPDATE warehouse_access_event
            SET attempt_count = :attemptCount, last_error = :lastError, next_attempt_at = :nextAttemptAt
            WHERE principal_id = :principalId AND event_id = :eventId
            """.trimIndent()
        )
            .bind("attemptCount", attemptCount)
            .bind("lastError", lastError.take(MAX_ERROR_LENGTH))
            .bind("nextAttemptAt", nextAttemptAt)
            .bind("principalId", principalId.toString())
            .bind("eventId", eventId)
            .fetch()
            .awaitRowsUpdated()
    }

    override suspend fun count(): Long {
        return databaseClient.sql("SELECT COUNT(*) AS cnt FROM warehouse_access_event")
            .map { row, _ -> requireNotNull(row.get("cnt", Number::class.java)).toLong() }
            .awaitOne()
    }

    private companion object {
        const val MAX_ERROR_LENGTH = 500
    }
}
