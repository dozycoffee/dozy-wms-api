package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.CallInventoryDirectlyUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryDirectCallResult
import com.dozycoffee.wms.outbound.domain.exception.OutboundInventoryRejectedException
import com.dozycoffee.wms.outbound.domain.exception.OutboundInventoryUnavailableException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class OutboundInventoryReallocatorTest {

    @Mock
    private lateinit var callInventoryDirectlyUseCase: CallInventoryDirectlyUseCase

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    private fun reallocator() = OutboundInventoryReallocator(callInventoryDirectlyUseCase, jsonMapper)

    private val shortages = listOf(OutboundInventoryReallocator.Shortage(55L, 2))

    @Test
    fun `부족 수량을 예약 재할당 경로로 보내고 응답의 새 할당을 읽는다`() = runTest {
        val response = """{"reservationId":7,"status":"CONFIRMED","heldInventoryIds":[55],"items":[{"productId":100,"allocations":[{"inventoryId":56,"lotId":8,"lotNumber":"B","expirationDate":"2027-02-01","quantity":4},{"inventoryId":57,"lotId":9,"lotNumber":"C","quantity":2}]}]}"""
        whenever(callInventoryDirectlyUseCase.post(any(), any(), any())).thenReturn(InventoryDirectCallResult.Accepted(response))

        val items = reallocator().reallocate(7L, "wms-reallocation-3-1", shortages)

        val body = argumentCaptor<String>()
        verify(callInventoryDirectlyUseCase).post(eq("/api/v1/reservations/7/reallocation"), eq("wms-reallocation-3-1"), body.capture())
        val json = jsonMapper.readTree(body.firstValue)
        assertThat(json.get("shortages").get(0).get("inventoryId").asLong()).isEqualTo(55L)
        assertThat(json.get("shortages").get(0).get("shortageQuantity").asInt()).isEqualTo(2)
        assertThat(items).hasSize(1)
        assertThat(items.first().productId).isEqualTo(100L)
        assertThat(items.first().allocations.map { it.inventoryId }).containsExactly(56L, 57L)
        assertThat(items.first().allocations.first().lotNumber).isEqualTo("B")
        assertThat(items.first().allocations.first().expirationDate).isEqualTo(LocalDate.of(2027, 2, 1))
        assertThat(items.first().allocations.last().expirationDate).isNull()
    }

    @Test
    fun `inventory가 거부하면 예외를 던진다`() {
        runBlocking {
            whenever(callInventoryDirectlyUseCase.post(any(), any(), any())).thenReturn(InventoryDirectCallResult.Rejected(409, "{}"))
        }

        assertThatThrownBy { runBlocking { reallocator().reallocate(7L, "k", shortages) } }
            .isInstanceOf(OutboundInventoryRejectedException::class.java)
    }

    @Test
    fun `inventory와 통신할 수 없으면 예외를 던진다`() {
        runBlocking {
            whenever(callInventoryDirectlyUseCase.post(any(), any(), any())).thenReturn(InventoryDirectCallResult.Unavailable("timeout"))
        }

        assertThatThrownBy { runBlocking { reallocator().reallocate(7L, "k", shortages) } }
            .isInstanceOf(OutboundInventoryUnavailableException::class.java)
    }
}
