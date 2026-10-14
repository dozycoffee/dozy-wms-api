package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.fixture.DisposalItemTestBuilder.Companion.disposalItem
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
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
class DisposalInventoryCallRecorderTest {

    @Mock
    private lateinit var recordInventoryCallUseCase: RecordInventoryCallUseCase

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    @Test
    fun `폐기 상품을 폐기 상품 ID 기반 멱등 키와 요청 본문으로 기록한다`() = runTest {
        val item = disposalItem().disposalItemId(41L).inventoryId(10L).quantity(5).build()
        val inventory = InventoryResult(10L, 100L, 1L, 200L, 5, 0, 5, QualityStatus.DISPOSAL_SCHEDULED)

        DisposalInventoryCallRecorder(recordInventoryCallUseCase, jsonMapper).record(7L, item, inventory, "LOT-1")

        val captor = argumentCaptor<RecordInventoryCallCommand>()
        verify(recordInventoryCallUseCase).record(captor.capture())
        val command = captor.firstValue
        assertThat(command.callType).isEqualTo(InventoryCallType.DISPOSAL)
        assertThat(command.warehouseId).isEqualTo(7L)
        assertThat(command.idempotencyKey).isEqualTo("wms-disposal-41")
        assertThat(command.requestPath).isEqualTo("/api/v1/disposals")
        val body = jsonMapper.readTree(command.requestBody)
        assertThat(body.get("productId").asLong()).isEqualTo(100L)
        assertThat(body.get("lotNumber").asText()).isEqualTo("LOT-1")
        assertThat(body.get("qualityStatus").asText()).isEqualTo("DISPOSAL_SCHEDULED")
        assertThat(body.get("quantity").asInt()).isEqualTo(5)
        assertThat(body.get("disposalItemId").asLong()).isEqualTo(41L)
    }

    @Test
    fun `불량 재고는 DEFECTIVE로 전달한다`() = runTest {
        val item = disposalItem().disposalItemId(42L).inventoryId(11L).quantity(2).build()
        val inventory = InventoryResult(11L, 100L, 1L, 200L, 2, 0, 2, QualityStatus.DEFECTIVE)

        DisposalInventoryCallRecorder(recordInventoryCallUseCase, jsonMapper).record(7L, item, inventory, "LOT-1")

        val captor = argumentCaptor<RecordInventoryCallCommand>()
        verify(recordInventoryCallUseCase).record(captor.capture())
        assertThat(jsonMapper.readTree(captor.firstValue.requestBody).get("qualityStatus").asText()).isEqualTo("DEFECTIVE")
    }
}
