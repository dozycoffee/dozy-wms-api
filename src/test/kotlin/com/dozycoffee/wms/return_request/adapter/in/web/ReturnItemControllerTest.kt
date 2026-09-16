package com.dozycoffee.wms.return_request.adapter.`in`.web

import com.dozycoffee.wms.return_request.adapter.`in`.web.request.InspectReturnItemRequest
import com.dozycoffee.wms.return_request.application.port.`in`.GetReturnItemUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.InspectReturnItemUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnItemResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
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

@WebFluxTest(ReturnItemController::class)
class ReturnItemControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var inspectReturnItemUseCase: InspectReturnItemUseCase

    @MockitoBean
    private lateinit var getReturnItemUseCase: GetReturnItemUseCase

    private fun sampleResult(inspectionResult: ReturnInspectionResult = ReturnInspectionResult.PENDING): ReturnItemResult {
        return ReturnItemResult(1L, 1L, 10L, 5, null, inspectionResult, null)
    }

    @Nested
    inner class 반품별_목록_조회 {

        @Test
        fun `반품 ID로 조회하면 200을 반환한다`() {
            whenever(getReturnItemUseCase.getAllByReturnRequest(1L)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/return-items?returnRequestId={returnRequestId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].returnItemId").isEqualTo(1)
        }
    }

    @Nested
    inner class 검수 {

        @Test
        fun `요청이 유효하면 200과 검수 결과를 반환한다`() {
            runBlocking {
                whenever(inspectReturnItemUseCase.inspect(any()))
                    .thenReturn(sampleResult(ReturnInspectionResult.NORMAL).copy(actualQuantity = 5))
            }

            webTestClient.patch().uri("/api/return-items/{returnItemId}/inspect", 1L)
                .bodyValue(InspectReturnItemRequest(5, ReturnInspectionResult.NORMAL))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.inspectionResult").isEqualTo("NORMAL")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.patch().uri("/api/return-items/{returnItemId}/inspect", 1L)
                .bodyValue(InspectReturnItemRequest(null, null))
                .exchange()
                .expectStatus().isBadRequest
        }
    }
}
