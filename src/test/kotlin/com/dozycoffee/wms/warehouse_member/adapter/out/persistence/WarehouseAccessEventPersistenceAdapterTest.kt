package com.dozycoffee.wms.warehouse_member.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.LocalDateTime
import java.util.UUID

@DataR2dbcTest
@Import(R2dbcConfig::class, SystemActorProvider::class, WarehouseAccessEventPersistenceAdapter::class)
class WarehouseAccessEventPersistenceAdapterTest {

    @Autowired
    private lateinit var adapter: WarehouseAccessEventPersistenceAdapter

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Autowired
    private lateinit var transactionManager: ReactiveTransactionManager

    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 13, 10, 0, 0)
    private val principalA: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")
    private val principalB: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d60")

    @AfterEach
    fun cleanUp() {
        runTest { databaseClient.sql("DELETE FROM warehouse_access_event").fetch().awaitRowsUpdated() }
    }

    @Nested
    inner class 기록 {

        @Test
        fun `처음 기록하면 즉시 보낼 수 있는 행이 생긴다`() = runTest {
            adapter.markChanged(principalA, "event-1", now.minusSeconds(3), now)

            val claimed = adapter.claimDue(now, now.plusMinutes(1), 10)

            assertThat(claimed).hasSize(1)
            assertThat(claimed[0].principalId).isEqualTo(principalA)
            assertThat(claimed[0].eventId).isEqualTo("event-1")
            assertThat(claimed[0].occurredAt).isEqualTo(now.minusSeconds(3))
            assertThat(claimed[0].attemptCount).isEqualTo(0)
        }

        @Test
        fun `같은 직원을 다시 기록하면 한 행으로 합쳐지고 eventId가 새로 바뀌며 시도 정보가 초기화된다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)
            adapter.recordFailure(principalA, "event-1", 3, "broker down", now.plusMinutes(5))

            adapter.markChanged(principalA, "event-2", now.plusSeconds(10), now.plusSeconds(10))

            assertThat(adapter.count()).isEqualTo(1)
            val claimed = adapter.claimDue(now.plusSeconds(10), now.plusMinutes(1), 10)
            assertThat(claimed.single().eventId).isEqualTo("event-2")
            assertThat(claimed.single().attemptCount).isEqualTo(0)
            assertThat(claimed.single().lastError).isNull()
        }

        @Test
        fun `직원이 다르면 각각 행이 생긴다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)
            adapter.markChanged(principalB, "event-2", now, now)

            assertThat(adapter.count()).isEqualTo(2)
        }
    }

    @Nested
    inner class 점유 {

        @Test
        fun `점유하면 다음 시도 시각이 lease만큼 미뤄져 다시 점유되지 않는다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)

            assertThat(adapter.claimDue(now, now.plusMinutes(1), 10)).hasSize(1)
            assertThat(adapter.claimDue(now.plusSeconds(30), now.plusMinutes(2), 10)).isEmpty()
            assertThat(adapter.claimDue(now.plusMinutes(1), now.plusMinutes(2), 10)).hasSize(1)
        }

        @Test
        fun `다음 시도 시각이 안 된 행은 점유하지 않는다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)
            adapter.recordFailure(principalA, "event-1", 1, "x", now.plusSeconds(5))

            assertThat(adapter.claimDue(now, now.plusMinutes(1), 10)).isEmpty()
            assertThat(adapter.claimDue(now.plusSeconds(5), now.plusMinutes(1), 10)).hasSize(1)
        }

        @Test
        fun `개수 제한을 지킨다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)
            adapter.markChanged(principalB, "event-2", now, now)

            assertThat(adapter.claimDue(now, now.plusMinutes(1), 1)).hasSize(1)
        }

        @Test
        fun `다른 트랜잭션이 잠근 행은 건너뛴다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)
            val transactionalOperator = TransactionalOperator.create(transactionManager)

            val (first, second) = transactionalOperator.executeAndAwait {
                val locked = adapter.claimDue(now, now.plusMinutes(1), 10)
                val seenBySecond = CoroutineScope(Dispatchers.Default).async {
                    transactionalOperator.executeAndAwait { adapter.claimDue(now, now.plusMinutes(1), 10) }
                }.await()
                locked to seenBySecond
            }

            assertThat(first).hasSize(1)
            assertThat(second).isEmpty()
        }
    }

    @Nested
    inner class 완료_처리 {

        @Test
        fun `eventId가 같으면 행을 지운다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)

            assertThat(adapter.deleteIfUnchanged(principalA, "event-1")).isTrue()
            assertThat(adapter.count()).isEqualTo(0)
        }

        @Test
        fun `그 사이 다시 변경되어 eventId가 바뀌었으면 지우지 않는다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)
            adapter.markChanged(principalA, "event-2", now.plusSeconds(1), now.plusSeconds(1))

            assertThat(adapter.deleteIfUnchanged(principalA, "event-1")).isFalse()
            assertThat(adapter.count()).isEqualTo(1)
        }

        @Test
        fun `실패 기록도 eventId가 바뀌었으면 반영하지 않는다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)
            adapter.markChanged(principalA, "event-2", now, now)

            adapter.recordFailure(principalA, "event-1", 5, "old", now.plusHours(1))

            val claimed = adapter.claimDue(now, now.plusMinutes(1), 10)
            assertThat(claimed.single().attemptCount).isEqualTo(0)
        }

        @Test
        fun `실패 기록은 시도 횟수와 오류를 남기고 오류는 500자로 자른다`() = runTest {
            adapter.markChanged(principalA, "event-1", now, now)
            adapter.recordFailure(principalA, "event-1", 2, "e".repeat(700), now)

            val claimed = adapter.claimDue(now, now.plusMinutes(1), 10)
            assertThat(claimed.single().attemptCount).isEqualTo(2)
            assertThat(claimed.single().lastError).hasSize(500)
        }
    }
}
