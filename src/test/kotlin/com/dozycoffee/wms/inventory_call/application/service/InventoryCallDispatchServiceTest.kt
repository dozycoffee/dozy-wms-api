package com.dozycoffee.wms.inventory_call.application.service

import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiClient
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiResult
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryCallRepository
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.NOW
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.inventoryCall
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class InventoryCallDispatchServiceTest {

    @Mock
    private lateinit var inventoryCallRepository: InventoryCallRepository

    @Mock
    private lateinit var inventoryApiClient: InventoryApiClient

    private lateinit var service: InventoryCallDispatchService

    @BeforeEach
    fun setUp() {
        val clock: Clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"))
        service = InventoryCallDispatchService(
            inventoryCallRepository, inventoryApiClient, clock, 20, Duration.ofSeconds(5), Duration.ofMinutes(10), 3
        )
    }

    private fun pending(id: Long, attempts: Int = 0): InventoryCall =
        inventoryCall().inventoryCallId(id).idempotencyKey("wms-inbound-receipt-$id").attemptCount(attempts).build()

    @Test
    fun `보낼 호출이 없으면 아무것도 하지 않는다`() = runTest {
        whenever(inventoryCallRepository.findDispatchable(eq(NOW), eq(20))).thenReturn(emptyFlow())

        val result = service.dispatchBatch()

        assertThat(result.processed).isEqualTo(0)
        verify(inventoryApiClient, never()).post(any(), any(), any())
    }

    @Test
    fun `2xx 응답이면 SENT로 바꾸고 응답을 저장한다`() = runTest {
        val call = pending(1L)
        whenever(inventoryCallRepository.findDispatchable(eq(NOW), eq(20))).thenReturn(flowOf(call))
        whenever(inventoryApiClient.post("/api/v1/inbound-receipts", "wms-inbound-receipt-1", "{\"quantity\":10}"))
            .thenReturn(InventoryApiResult.Success("{\"quantityAfter\":10}"))
        whenever(inventoryCallRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.dispatchBatch()

        assertThat(result.sent).isEqualTo(1)
        assertThat(call.status).isEqualTo(InventoryCallStatus.SENT)
        assertThat(call.responseBody).isEqualTo("{\"quantityAfter\":10}")
        verify(inventoryCallRepository).save(call)
    }

    @Nested
    inner class 재시도_대상_실패 {

        @Test
        fun `첫 실패는 기본 대기 시간만큼 미룬다`() = runTest {
            val call = pending(1L)
            whenever(inventoryCallRepository.findDispatchable(eq(NOW), eq(20))).thenReturn(flowOf(call))
            whenever(inventoryApiClient.post(any(), any(), any())).thenReturn(InventoryApiResult.Retryable("HTTP 503"))
            whenever(inventoryCallRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = service.dispatchBatch()

            assertThat(result.retrying).isEqualTo(1)
            assertThat(call.status).isEqualTo(InventoryCallStatus.PENDING)
            assertThat(call.attemptCount).isEqualTo(1)
            assertThat(call.nextAttemptAt).isEqualTo(NOW.plusSeconds(5))
            assertThat(call.lastError).isEqualTo("HTTP 503")
        }

        @Test
        fun `대기 시간은 시도마다 두 배가 되고 최대값을 넘지 않는다`() = runTest {
            val third = pending(1L, attempts = 2)
            val huge = pending(2L, attempts = 30)
            whenever(inventoryCallRepository.findDispatchable(eq(NOW), eq(20))).thenReturn(flowOf(third, huge))
            whenever(inventoryApiClient.post(any(), any(), any())).thenReturn(InventoryApiResult.Retryable("timeout"))
            whenever(inventoryCallRepository.save(any())).thenAnswer { it.getArgument(0) }

            service.dispatchBatch()

            assertThat(third.nextAttemptAt).isEqualTo(NOW.plusSeconds(20))
            assertThat(huge.nextAttemptAt).isEqualTo(NOW.plusMinutes(10))
        }

        @Test
        fun `시도 횟수가 많아도 DEAD로 격리하지 않는다`() = runTest {
            val call = pending(1L, attempts = 100)
            whenever(inventoryCallRepository.findDispatchable(eq(NOW), eq(20))).thenReturn(flowOf(call))
            whenever(inventoryApiClient.post(any(), any(), any())).thenReturn(InventoryApiResult.Retryable("timeout"))
            whenever(inventoryCallRepository.save(any())).thenAnswer { it.getArgument(0) }

            service.dispatchBatch()

            assertThat(call.status).isEqualTo(InventoryCallStatus.PENDING)
            assertThat(call.attemptCount).isEqualTo(101)
        }
    }

    @Test
    fun `다시 보내도 소용없는 실패면 DEAD로 격리한다`() = runTest {
        val call = pending(1L)
        whenever(inventoryCallRepository.findDispatchable(eq(NOW), eq(20))).thenReturn(flowOf(call))
        whenever(inventoryApiClient.post(any(), any(), any()))
            .thenReturn(InventoryApiResult.Permanent(404, "{\"code\":\"INV_PRODUCT_NOT_FOUND\"}", "HTTP 404"))
        whenever(inventoryCallRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.dispatchBatch()

        assertThat(result.dead).isEqualTo(1)
        assertThat(call.status).isEqualTo(InventoryCallStatus.DEAD)
        assertThat(call.deadAt).isEqualTo(NOW)
        assertThat(call.responseBody).contains("INV_PRODUCT_NOT_FOUND")
        val captor = argumentCaptor<InventoryCall>()
        verify(inventoryCallRepository).save(captor.capture())
        assertThat(captor.firstValue.lastError).isEqualTo("HTTP 404")
    }

    @Test
    fun `한 배치의 여러 창고 호출을 각각 처리한다`() = runTest {
        val a = pending(1L)
        val b = inventoryCall().inventoryCallId(2L).warehouseId(2L).idempotencyKey("wms-inbound-receipt-2").build()
        whenever(inventoryCallRepository.findDispatchable(eq(NOW), eq(20))).thenReturn(flowOf(a, b))
        whenever(inventoryApiClient.post(any(), eq("wms-inbound-receipt-1"), any())).thenReturn(InventoryApiResult.Success(null))
        whenever(inventoryApiClient.post(any(), eq("wms-inbound-receipt-2"), any()))
            .thenReturn(InventoryApiResult.Permanent(400, null, "HTTP 400"))
        whenever(inventoryCallRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.dispatchBatch()

        assertThat(result.sent).isEqualTo(1)
        assertThat(result.dead).isEqualTo(1)
        assertThat(a.status).isEqualTo(InventoryCallStatus.SENT)
        assertThat(b.status).isEqualTo(InventoryCallStatus.DEAD)
    }
}
