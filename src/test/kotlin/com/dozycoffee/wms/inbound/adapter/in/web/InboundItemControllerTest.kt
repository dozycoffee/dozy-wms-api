package com.dozycoffee.wms.inbound.adapter.`in`.web

import com.dozycoffee.wms.inbound.adapter.`in`.web.request.InspectInboundItemRequest
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.InspectInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundItemResult
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.InboundItemAlreadyInspectedException
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

@WebFluxTest(InboundItemController::class)
class InboundItemControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var inspectInboundItemUseCase: InspectInboundItemUseCase

    @MockitoBean
    private lateinit var getInboundItemUseCase: GetInboundItemUseCase

    private fun sampleResult(inspectionResult: InspectionResult = InspectionResult.PENDING): InboundItemResult {
        return InboundItemResult(1L, 1L, 100L, 10L, 30, null, inspectionResult, null)
    }

    @Nested
    inner class 검수 {

        @Test
        fun `유효한 요청이면 200과 검수 결과를 반환한다`() {
            runBlocking {
                whenever(inspectInboundItemUseCase.inspect(any())).thenReturn(sampleResult(InspectionResult.NORMAL))
            }

            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(30, InspectionResult.NORMAL))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.inspectionResult").isEqualTo("NORMAL")
        }

        @Test
        fun `이미 검수된 상품이면 409를 반환한다`() {
            runBlocking {
                whenever(inspectInboundItemUseCase.inspect(any())).thenThrow(InboundItemAlreadyInspectedException())
            }

            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(30, InspectionResult.NORMAL))
                .exchange()
                .expectStatus().isEqualTo(409)
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.patch().uri("/api/inbound-items/{inboundItemId}/inspect", 1L)
                .bodyValue(InspectInboundItemRequest(null, null))
                .exchange()
                .expectStatus().isBadRequest
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
