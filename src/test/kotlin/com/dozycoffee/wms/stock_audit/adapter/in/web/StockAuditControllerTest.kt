package com.dozycoffee.wms.stock_audit.adapter.`in`.web

import com.dozycoffee.wms.stock_audit.adapter.`in`.web.request.AssignStockAuditRequest
import com.dozycoffee.wms.stock_audit.adapter.`in`.web.request.CloseStockAuditRequest
import com.dozycoffee.wms.stock_audit.adapter.`in`.web.request.RegisterStockAuditRequest
import com.dozycoffee.wms.stock_audit.application.port.`in`.AssignStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CloseStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CompleteStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.GetStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.RegisterStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditNotFoundException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(StockAuditController::class)
class StockAuditControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerStockAuditUseCase: RegisterStockAuditUseCase

    @MockitoBean
    private lateinit var getStockAuditUseCase: GetStockAuditUseCase

    @MockitoBean
    private lateinit var assignStockAuditUseCase: AssignStockAuditUseCase

    @MockitoBean
    private lateinit var completeStockAuditUseCase: CompleteStockAuditUseCase

    @MockitoBean
    private lateinit var closeStockAuditUseCase: CloseStockAuditUseCase

    private fun sampleResult(status: StockAuditStatus = StockAuditStatus.SCHEDULED): StockAuditResult {
        return StockAuditResult(1L, 1L, 10L, status, null, null)
    }

    @Nested
    inner class 실사_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 실사를 반환한다`() {
            runBlocking { whenever(registerStockAuditUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/stock-audits")
                .bodyValue(RegisterStockAuditRequest(1L, 10L))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.stockAuditId").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("SCHEDULED")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/stock-audits")
                .bodyValue(RegisterStockAuditRequest(null, null))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 실사_단건_조회 {

        @Test
        fun `존재하면 200을 반환한다`() {
            runBlocking { whenever(getStockAuditUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/stock-audits/{stockAuditId}", 1L)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getStockAuditUseCase.getById(eq(999L))).thenThrow(StockAuditNotFoundException()) }

            webTestClient.get().uri("/api/stock-audits/{stockAuditId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 실사_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200을 반환한다`() {
            whenever(getStockAuditUseCase.getAll(null, null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/stock-audits")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].stockAuditId").isEqualTo(1)
        }
    }

    @Nested
    inner class 담당자_배정 {

        @Test
        fun `요청이 유효하면 200과 IN_PROGRESS 상태를 반환한다`() {
            runBlocking {
                whenever(assignStockAuditUseCase.assign(1L, "담당자A"))
                    .thenReturn(sampleResult(StockAuditStatus.IN_PROGRESS))
            }

            webTestClient.patch().uri("/api/stock-audits/{stockAuditId}/assign", 1L)
                .bodyValue(AssignStockAuditRequest("담당자A"))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("IN_PROGRESS")
        }

        @Test
        fun `담당자가 비어있으면 400을 반환한다`() {
            webTestClient.patch().uri("/api/stock-audits/{stockAuditId}/assign", 1L)
                .bodyValue(AssignStockAuditRequest(""))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 실사_완료 {

        @Test
        fun `요청이 유효하면 200과 COMPLETED 상태를 반환한다`() {
            runBlocking {
                whenever(completeStockAuditUseCase.complete(1L)).thenReturn(sampleResult(StockAuditStatus.COMPLETED))
            }

            webTestClient.patch().uri("/api/stock-audits/{stockAuditId}/complete", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED")
        }
    }

    @Nested
    inner class 실사_마감 {

        @Test
        fun `승인자 없이 요청하면 200과 CLOSED 상태를 반환한다`() {
            runBlocking {
                whenever(closeStockAuditUseCase.close(1L, null)).thenReturn(sampleResult(StockAuditStatus.CLOSED))
            }

            webTestClient.patch().uri("/api/stock-audits/{stockAuditId}/close", 1L)
                .bodyValue(CloseStockAuditRequest(null))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("CLOSED")
        }

        @Test
        fun `본문 없이 요청해도 200을 반환한다`() {
            runBlocking {
                whenever(closeStockAuditUseCase.close(1L, null)).thenReturn(sampleResult(StockAuditStatus.CLOSED))
            }

            webTestClient.patch().uri("/api/stock-audits/{stockAuditId}/close", 1L)
                .exchange()
                .expectStatus().isOk
        }
    }
}
