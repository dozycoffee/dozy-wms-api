package com.dozycoffee.wms.inventory_call.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryCallRepository
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.inventory_call.domain.exception.InvalidInventoryCallStateException
import com.dozycoffee.wms.inventory_call.domain.exception.InventoryCallKeyConflictException
import com.dozycoffee.wms.inventory_call.domain.exception.InventoryCallNotFoundException
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.NOW
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.inventoryCall
import com.dozycoffee.wms.support.duplicateKeyViolation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class InventoryCallServiceTest {

    @Mock
    private lateinit var inventoryCallRepository: InventoryCallRepository

    private lateinit var service: InventoryCallService

    private val command = RecordInventoryCallCommand(
        InventoryCallType.INBOUND_RECEIPT, 1L, "wms-inbound-receipt-1", "/api/v1/inbound-receipts", "{\"quantity\":10}"
    )

    @BeforeEach
    fun setUp() {
        val clock: Clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"))
        service = InventoryCallService(inventoryCallRepository, clock)
    }

    private fun savedWithId(): InventoryCall = inventoryCall().inventoryCallId(7L).build()

    @Nested
    inner class 기록 {

        @Test
        fun `새 키면 PENDING 호출을 저장한다`() = runTest {
            whenever(inventoryCallRepository.findByIdempotencyKey("wms-inbound-receipt-1")).thenReturn(null)
            whenever(inventoryCallRepository.save(any())).thenReturn(savedWithId())

            val result = service.record(command)

            assertThat(result.inventoryCallId).isEqualTo(7L)
            assertThat(result.status).isEqualTo(InventoryCallStatus.PENDING)
        }

        @Test
        fun `같은 키와 같은 내용이면 저장하지 않고 기존 행을 돌려준다`() = runTest {
            whenever(inventoryCallRepository.findByIdempotencyKey("wms-inbound-receipt-1")).thenReturn(savedWithId())

            val result = service.record(command)

            assertThat(result.inventoryCallId).isEqualTo(7L)
            verify(inventoryCallRepository, never()).save(any())
        }

        @Test
        fun `같은 키에 다른 내용이면 충돌 예외를 던진다`() = runTest {
            whenever(inventoryCallRepository.findByIdempotencyKey("wms-inbound-receipt-1")).thenReturn(savedWithId())

            assertThatThrownBy { runBlocking { service.record(command.copy(requestBody = "{\"quantity\":99}")) } }
                .isInstanceOf(InventoryCallKeyConflictException::class.java)
            verify(inventoryCallRepository, never()).save(any())
        }

        @Test
        fun `동시 기록으로 유니크 제약에 걸리면 충돌 예외로 바꾼다`() = runTest {
            whenever(inventoryCallRepository.findByIdempotencyKey("wms-inbound-receipt-1")).thenReturn(null)
            whenever(inventoryCallRepository.save(any())).thenThrow(duplicateKeyViolation())

            assertThatThrownBy { runBlocking { service.record(command) } }
                .isInstanceOf(InventoryCallKeyConflictException::class.java)
        }
    }

    @Nested
    inner class 조회 {

        @Test
        fun `없는 호출을 조회하면 예외를 던진다`() = runTest {
            whenever(inventoryCallRepository.findById(9L)).thenReturn(null)

            assertThatThrownBy { runBlocking { service.getById(9L) } }.isInstanceOf(InventoryCallNotFoundException::class.java)
        }

        @Test
        fun `상태로 걸러 목록을 조회한다`() = runTest {
            whenever(inventoryCallRepository.findAll(InventoryCallStatus.DEAD, 10)).thenReturn(flowOf(savedWithId()))

            assertThat(service.getAll(InventoryCallStatus.DEAD, 10).toList()).hasSize(1)
        }
    }

    @Nested
    inner class 격리_호출_처리 {

        private fun deadCall(): InventoryCall =
            inventoryCall().inventoryCallId(7L).status(InventoryCallStatus.DEAD).attemptCount(1).lastError("HTTP 404").build()

        @Test
        fun `재전송하면 PENDING으로 되돌려 저장한다`() = runTest {
            whenever(inventoryCallRepository.findById(7L)).thenReturn(deadCall())
            whenever(inventoryCallRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = service.requeue(7L)

            assertThat(result.status).isEqualTo(InventoryCallStatus.PENDING)
            assertThat(result.attemptCount).isEqualTo(0)
            assertThat(result.nextAttemptAt).isEqualTo(NOW)
        }

        @Test
        fun `버리면 DISCARDED로 저장한다`() = runTest {
            whenever(inventoryCallRepository.findById(7L)).thenReturn(deadCall())
            whenever(inventoryCallRepository.save(any())).thenAnswer { it.getArgument(0) }

            assertThat(service.discard(7L).status).isEqualTo(InventoryCallStatus.DISCARDED)
        }

        @Test
        fun `DEAD가 아닌 호출은 재전송할 수 없다`() = runTest {
            whenever(inventoryCallRepository.findById(7L)).thenReturn(savedWithId())

            assertThatThrownBy { runBlocking { service.requeue(7L) } }
                .isInstanceOf(InvalidInventoryCallStateException::class.java)
            verify(inventoryCallRepository, never()).save(any())
        }

        @Test
        fun `없는 호출은 재전송할 수 없다`() = runTest {
            whenever(inventoryCallRepository.findById(9L)).thenReturn(null)

            assertThatThrownBy { runBlocking { service.requeue(9L) } }.isInstanceOf(InventoryCallNotFoundException::class.java)
        }
    }
}
