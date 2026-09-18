package com.dozycoffee.wms.stock_audit.adapter.`in`.web

import com.dozycoffee.wms.stock_audit.adapter.`in`.web.request.CountStockAuditItemRequest
import com.dozycoffee.wms.stock_audit.application.port.`in`.CountStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.GetStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.CountStockAuditItemCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditItemResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(StockAuditItemController::class)
class StockAuditItemControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getStockAuditItemUseCase: GetStockAuditItemUseCase

    @MockitoBean
    private lateinit var countStockAuditItemUseCase: CountStockAuditItemUseCase

    private fun sampleResult(): StockAuditItemResult {
        return StockAuditItemResult(1L, 1L, 100L, 20, 18, -2, false)
    }

    @Nested
    inner class 실사별_항목_목록_조회 {

        @Test
        fun `stockAuditId로 조회하면 200과 항목 목록을 반환한다`() {
            whenever(getStockAuditItemUseCase.getAllByStockAudit(1L)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri { it.path("/api/stock-audit-items").queryParam("stockAuditId", 1L).build() }
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].stockAuditItemId").isEqualTo(1)
                .jsonPath("$[0].discrepancy").isEqualTo(-2)
        }
    }

    @Nested
    inner class 실측_수량_입력 {

        @Test
        fun `유효한 요청이면 200과 갱신된 항목을 반환한다`() {
            runBlocking {
                whenever(countStockAuditItemUseCase.count(CountStockAuditItemCommand(1L, 18)))
                    .thenReturn(sampleResult())
            }

            webTestClient.patch().uri("/api/stock-audit-items/{stockAuditItemId}/count", 1L)
                .bodyValue(CountStockAuditItemRequest(18))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.countedQuantity").isEqualTo(18)
        }

        @Test
        fun `실측 수량이 없으면 400을 반환한다`() {
            webTestClient.patch().uri("/api/stock-audit-items/{stockAuditItemId}/count", 1L)
                .bodyValue(CountStockAuditItemRequest(null))
                .exchange()
                .expectStatus().isBadRequest
        }
    }
}
