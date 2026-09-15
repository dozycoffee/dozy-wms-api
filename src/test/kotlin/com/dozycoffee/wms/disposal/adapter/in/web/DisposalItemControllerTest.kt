package com.dozycoffee.wms.disposal.adapter.`in`.web

import com.dozycoffee.wms.disposal.application.port.`in`.GetDisposalItemUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalItemResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(DisposalItemController::class)
class DisposalItemControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getDisposalItemUseCase: GetDisposalItemUseCase

    private fun sampleResult(): DisposalItemResult {
        return DisposalItemResult(1L, 1L, 10L, 5, DisposalReason.EXPIRED)
    }

    @Nested
    inner class 폐기별_목록_조회 {

        @Test
        fun `폐기 ID로 조회하면 200을 반환한다`() {
            whenever(getDisposalItemUseCase.getAllByDisposal(1L)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/disposal-items?disposalId={disposalId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].disposalItemId").isEqualTo(1)
        }
    }
}
