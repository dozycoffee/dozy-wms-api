package com.dozycoffee.wms.inbound.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.inbound.adapter.`in`.web.request.InboundReceiptRequest
import com.dozycoffee.wms.inbound.adapter.`in`.web.request.InspectInboundItemRequest
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.InspectInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundItemResult
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundReceiptResult
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inbound.domain.exception.InboundItemAlreadyInspectedException
import com.dozycoffee.wms.inbound.domain.exception.InboundOverReceivedException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WithDozyPrincipal(roles = ["wms:inbound_manager"])
@WebFluxTest(InboundItemController::class)
class InboundItemControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var inspectInboundItemUseCase: InspectInboundItemUseCase

    @MockitoBean
    private lateinit var getInboundItemUseCase: GetInboundItemUseCase

    private fun sampleResult(inspectionStatus: InspectionStatus = InspectionStatus.PENDING): InboundItemResult {
        val receipts = if (inspectionStatus == InspectionStatus.INSPECTED) {
            listOf(InboundReceiptResult(5L, "LOT-1", null, null, 30, InspectionResult.NORMAL, null))
        } else {
            emptyList()
        }
        return InboundItemResult(1L, 1L, 100L, 10L, 30, null, null, null, inspectionStatus, null, receipts)
    }

    private fun validReceipt(): InboundReceiptRequest =
        InboundReceiptRequest("LOT-1", null, null, 30, InspectionResult.NORMAL, null)

    @Nested
    inner class 검수 {

        @Test
        fun `유효한 요청이면 200과 검수 결과를 반환한다`() {
            runBlocking {
                whenever(inspectInboundItemUseCase.inspect(any())).thenReturn(sampleResult(InspectionStatus.INSPECTED))
            }

            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(listOf(validReceipt())))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.inspectionStatus").isEqualTo("INSPECTED")
                .jsonPath("$.receipts[0].lotNumber").isEqualTo("LOT-1")
                .jsonPath("$.receipts[0].inspectionResult").isEqualTo("NORMAL")
        }

        @Test
        fun `이미 검수된 상품이면 409를 반환한다`() {
            runBlocking {
                whenever(inspectInboundItemUseCase.inspect(any())).thenThrow(InboundItemAlreadyInspectedException())
            }

            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(listOf(validReceipt())))
                .exchange()
                .expectStatus().isEqualTo(409)
        }

        @Test
        fun `수령 라인 목록이 없으면 400을 반환한다`() {
            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(null))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `수령 라인의 필수값이 비어있으면 400을 반환한다`() {
            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(listOf(InboundReceiptRequest(" ", null, null, 0, null, null))))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `수령 라인이 비어 있는 미도착 검수는 허용한다`() {
            runBlocking {
                whenever(inspectInboundItemUseCase.inspect(any())).thenReturn(sampleResult(InspectionStatus.INSPECTED))
            }

            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(emptyList()))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `수량이 예정을 초과하면 409를 반환한다`() {
            runBlocking {
                whenever(inspectInboundItemUseCase.inspect(any())).thenThrow(InboundOverReceivedException())
            }

            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(listOf(validReceipt())))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INBOUND_RECEIPT_OVER_RECEIVED")
        }
    }

    @Nested
    inner class 입고별_목록_조회 {

        @Test
        fun `입고 ID로 조회하면 200을 반환한다`() {
            whenever(getInboundItemUseCase.getAllByInbound(1L)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/inbound-items?inboundId={inboundId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].inboundItemId").isEqualTo(1)
        }
    }
}
