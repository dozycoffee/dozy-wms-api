package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import tools.jackson.databind.json.JsonMapper

@ExtendWith(MockitoExtension::class)
class OutboundFulfillmentCallRecorderTest {

    @Mock
    private lateinit var recordInventoryCallUseCase: RecordInventoryCallUseCase

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    private fun recorder() = OutboundFulfillmentCallRecorder(recordInventoryCallUseCase, jsonMapper)

    @Test
    fun `모든 할당의 실출고 수량을 예약 출고 확정 한 건으로 기록한다 - 피킹하지 못한 할당은 0이다`() = runTest {
        val outbound = outbound().outboundId(3L).warehouseId(1L).reservationId(7L).build()
        val allocations = listOf(
            OutboundAllocation.reconstitute(1L, 30L, 55L, "A", null, 4, 4),
            OutboundAllocation.reconstitute(2L, 30L, 56L, "B", null, 2, 0),
            OutboundAllocation.reconstitute(3L, 31L, 57L, "C", null, 3, null)
        )

        recorder().record(outbound, allocations)

        val captor = argumentCaptor<RecordInventoryCallCommand>()
        verify(recordInventoryCallUseCase).record(captor.capture())
        val command = captor.firstValue
        assertThat(command.callType).isEqualTo(InventoryCallType.FULFILLMENT)
        assertThat(command.warehouseId).isEqualTo(1L)
        assertThat(command.idempotencyKey).isEqualTo("wms-fulfillment-3")
        assertThat(command.requestPath).isEqualTo("/api/v1/reservations/7/fulfillment")
        val items = jsonMapper.readTree(command.requestBody).get("allocations")
        assertThat(items.size()).isEqualTo(3)
        assertThat(items.get(0).get("inventoryId").asLong()).isEqualTo(55L)
        assertThat(items.get(0).get("shippedQuantity").asInt()).isEqualTo(4)
        assertThat(items.get(1).get("shippedQuantity").asInt()).isEqualTo(0)
        assertThat(items.get(2).get("shippedQuantity").asInt()).isEqualTo(0)
    }

    @Test
    fun `예약이 없는 이전 출고는 기록하지 않는다`() = runTest {
        val outbound = outbound().outboundId(3L).warehouseId(1L).reservationId(null).build()

        recorder().record(outbound, emptyList())

        verify(recordInventoryCallUseCase, never()).record(any())
    }
}
