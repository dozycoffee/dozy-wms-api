package com.dozycoffee.wms.outbound.adapter.`in`.web

import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundItemUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundItemResult
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(OutboundItemController::class)
class OutboundItemControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getOutboundItemUseCase: GetOutboundItemUseCase

    private fun sampleResult(): OutboundItemResult {
        return OutboundItemResult(1L, 1L, 100L, 15, null, null)
    }

    @Nested
    inner class 출고별_목록_조회 {

        @Test
        fun `출고 ID로 조회하면 200을 반환한다`() {
            whenever(getOutboundItemUseCase.getAllByOutbound(1L)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/outbound-items?outboundId={outboundId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].outboundItemId").isEqualTo(1)
        }
    }
}
