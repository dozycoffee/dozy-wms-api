package com.dozycoffee.wms.warehouse_member.application.service

import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventPublisher
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventRepository
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseAccessEvent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.ReactiveTransaction
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class WarehouseAccessEventServiceTest {

    @Mock
    private lateinit var warehouseAccessEventRepository: WarehouseAccessEventRepository

    @Mock
    private lateinit var warehouseMemberRepository: WarehouseMemberRepository

    @Mock
    private lateinit var warehouseAccessEventPublisher: WarehouseAccessEventPublisher

    private lateinit var service: WarehouseAccessEventService

    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 13, 10, 0, 0)
    private val principalId: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")
    private val occurredAt: LocalDateTime = now.minusMinutes(1)

    @BeforeEach
    fun setUp() {
        val transactionManager: ReactiveTransactionManager = mock()
        lenient().`when`(transactionManager.getReactiveTransaction(any())).thenReturn(Mono.just(mock<ReactiveTransaction>()))
        lenient().`when`(transactionManager.commit(any())).thenReturn(Mono.empty())
        val zone: ZoneId = ZoneId.of("Asia/Seoul")
        service = WarehouseAccessEventService(
            warehouseAccessEventRepository,
            warehouseMemberRepository,
            warehouseAccessEventPublisher,
            TransactionalOperator.create(transactionManager),
            Clock.fixed(now.atZone(zone).toInstant(), zone),
            50,
            Duration.ofMinutes(1),
            Duration.ofSeconds(5),
            Duration.ofMinutes(5),
            3
        )
    }

    private fun pending(attempts: Int = 0): WarehouseAccessEvent =
        WarehouseAccessEvent(principalId, "event-1", occurredAt, attempts, now, null)

    @Nested
    inner class 발행 {

        @Test
        fun `보낼 행이 없으면 아무것도 하지 않는다`() = runTest {
            whenever(warehouseAccessEventRepository.claimDue(eq(now), eq(now.plusMinutes(1)), eq(50))).thenReturn(emptyList())

            val result = service.publishBatch()

            assertThat(result.processed).isEqualTo(0)
            verify(warehouseAccessEventPublisher, never()).publish(any(), any(), any(), any())
        }

        @Test
        fun `발행 시점의 소속을 정렬해 같은 eventId로 보내고 성공하면 행을 지운다`() = runTest {
            whenever(warehouseAccessEventRepository.claimDue(eq(now), eq(now.plusMinutes(1)), eq(50))).thenReturn(listOf(pending()))
            whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(principalId)).thenReturn(setOf(30L, 10L, 20L))

            val result = service.publishBatch()

            assertThat(result.published).isEqualTo(1)
            verify(warehouseAccessEventPublisher).publish(principalId, "event-1", occurredAt, listOf(10L, 20L, 30L))
            verify(warehouseAccessEventRepository).deleteIfUnchanged(principalId, "event-1")
        }

        @Test
        fun `소속이 하나도 없으면 빈 목록을 보낸다`() = runTest {
            whenever(warehouseAccessEventRepository.claimDue(eq(now), eq(now.plusMinutes(1)), eq(50))).thenReturn(listOf(pending()))
            whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(principalId)).thenReturn(emptySet())

            service.publishBatch()

            verify(warehouseAccessEventPublisher).publish(principalId, "event-1", occurredAt, emptyList())
        }

        @Test
        fun `실패하면 행을 지우지 않고 시도 횟수와 백오프를 기록한다`() = runTest {
            whenever(warehouseAccessEventRepository.claimDue(eq(now), eq(now.plusMinutes(1)), eq(50))).thenReturn(listOf(pending()))
            whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(principalId)).thenReturn(setOf(10L))
            whenever(warehouseAccessEventPublisher.publish(any(), any(), any(), any())).thenThrow(IllegalStateException("broker down"))

            val result = service.publishBatch()

            assertThat(result.failed).isEqualTo(1)
            verify(warehouseAccessEventRepository, never()).deleteIfUnchanged(any(), any())
            verify(warehouseAccessEventRepository).recordFailure(
                eq(principalId), eq("event-1"), eq(1), eq("IllegalStateException: broker down"), eq(now.plusSeconds(5))
            )
        }

        @Test
        fun `대기 시간은 시도마다 두 배가 되고 최대값을 넘지 않으며 격리하지 않는다`() = runTest {
            whenever(warehouseAccessEventRepository.claimDue(eq(now), eq(now.plusMinutes(1)), eq(50)))
                .thenReturn(listOf(pending(attempts = 2)))
                .thenReturn(listOf(pending(attempts = 40)))
            whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(principalId)).thenReturn(setOf(10L))
            whenever(warehouseAccessEventPublisher.publish(any(), any(), any(), any())).thenThrow(IllegalStateException("x"))

            service.publishBatch()
            service.publishBatch()

            verify(warehouseAccessEventRepository).recordFailure(any(), any(), eq(3), any(), eq(now.plusSeconds(20)))
            verify(warehouseAccessEventRepository).recordFailure(any(), any(), eq(41), any(), eq(now.plusMinutes(5)))
        }

        @Test
        fun `한 직원의 실패가 다른 직원의 발행을 막지 않는다`() = runTest {
            val other = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d60")
            whenever(warehouseAccessEventRepository.claimDue(eq(now), eq(now.plusMinutes(1)), eq(50)))
                .thenReturn(listOf(pending(), WarehouseAccessEvent(other, "event-2", occurredAt, 0, now, null)))
            whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(principalId)).thenReturn(setOf(10L))
            whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(other)).thenReturn(setOf(20L))
            whenever(warehouseAccessEventPublisher.publish(eq(principalId), any(), any(), any())).thenThrow(IllegalStateException("x"))

            val result = service.publishBatch()

            assertThat(result.published).isEqualTo(1)
            assertThat(result.failed).isEqualTo(1)
            verify(warehouseAccessEventRepository).deleteIfUnchanged(other, "event-2")
        }
    }

    @Nested
    inner class 전체_재발행 {

        @Test
        fun `소속이 있는 모든 직원의 발행 대기를 새 eventId로 만든다`() = runTest {
            val other = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d60")
            whenever(warehouseMemberRepository.findAllPrincipalIds()).thenReturn(listOf(principalId, other))

            val count = service.republishAll()

            assertThat(count).isEqualTo(2)
            verify(warehouseAccessEventRepository).markChanged(eq(principalId), any(), eq(now), eq(now))
            verify(warehouseAccessEventRepository).markChanged(eq(other), any(), eq(now), eq(now))
        }

        @Test
        fun `소속이 있는 직원이 없으면 아무것도 만들지 않는다`() = runTest {
            whenever(warehouseMemberRepository.findAllPrincipalIds()).thenReturn(emptyList())

            assertThat(service.republishAll()).isEqualTo(0)
            verify(warehouseAccessEventRepository, never()).markChanged(any(), any(), any(), any())
        }
    }
}
