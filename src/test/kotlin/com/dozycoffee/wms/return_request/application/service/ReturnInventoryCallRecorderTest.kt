package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.fixture.ReturnItemTestBuilder.Companion.returnItem
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import tools.jackson.databind.json.JsonMapper

@ExtendWith(MockitoExtension::class)
class ReturnInventoryCallRecorderTest {

    @Mock
    private lateinit var recordInventoryCallUseCase: RecordInventoryCallUseCase

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    @Test
    fun `반품 상품을 반품 ID 기반 멱등 키와 요청 본문으로 기록한다`() = runTest {
        val item = returnItem().returnItemId(31L).productId(100L).actualQuantity(4)
            .inspectionResult(ReturnInspectionResult.NORMAL).build()

        ReturnInventoryCallRecorder(recordInventoryCallUseCase, jsonMapper).record(7L, item, "LOT-1")

        val captor = argumentCaptor<RecordInventoryCallCommand>()
        verify(recordInventoryCallUseCase).record(captor.capture())
        val command = captor.firstValue
        assertThat(command.callType).isEqualTo(InventoryCallType.RETURN)
        assertThat(command.warehouseId).isEqualTo(7L)
        assertThat(command.idempotencyKey).isEqualTo("wms-return-31")
        assertThat(command.requestPath).isEqualTo("/api/v1/returns")
        val body = jsonMapper.readTree(command.requestBody)
        assertThat(body.get("warehouseId").asLong()).isEqualTo(7L)
        assertThat(body.get("productId").asLong()).isEqualTo(100L)
        assertThat(body.get("lotNumber").asText()).isEqualTo("LOT-1")
        assertThat(body.get("qualityStatus").asText()).isEqualTo("NORMAL")
        assertThat(body.get("quantity").asInt()).isEqualTo(4)
        assertThat(body.get("returnItemId").asLong()).isEqualTo(31L)
    }

    @Test
    fun `불량 판정은 DEFECTIVE로 전달한다`() = runTest {
        val item = returnItem().returnItemId(32L).productId(100L).actualQuantity(2)
            .inspectionResult(ReturnInspectionResult.DEFECTIVE).build()

        ReturnInventoryCallRecorder(recordInventoryCallUseCase, jsonMapper).record(7L, item, "LOT-1")

        val captor = argumentCaptor<RecordInventoryCallCommand>()
        verify(recordInventoryCallUseCase).record(captor.capture())
        assertThat(jsonMapper.readTree(captor.firstValue.requestBody).get("qualityStatus").asText()).isEqualTo("DEFECTIVE")
    }
}
