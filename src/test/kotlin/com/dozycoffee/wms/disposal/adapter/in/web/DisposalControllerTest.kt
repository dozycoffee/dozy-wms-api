package com.dozycoffee.wms.disposal.adapter.`in`.web

import com.dozycoffee.wms.disposal.adapter.`in`.web.request.RegisterDisposalItemRequest
import com.dozycoffee.wms.disposal.adapter.`in`.web.request.RegisterDisposalRequest
import com.dozycoffee.wms.disposal.application.port.`in`.ApproveDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.CompleteDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.GetDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.exception.DisposalNotFoundException
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

@WebFluxTest(DisposalController::class)
class DisposalControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerDisposalUseCase: RegisterDisposalUseCase

    @MockitoBean
    private lateinit var approveDisposalUseCase: ApproveDisposalUseCase

    @MockitoBean
    private lateinit var completeDisposalUseCase: CompleteDisposalUseCase

    @MockitoBean
    private lateinit var getDisposalUseCase: GetDisposalUseCase

    private fun sampleResult(status: DisposalStatus = DisposalStatus.REQUESTED): DisposalResult {
        return DisposalResult(1L, 1L, status)
    }

    @Nested
    inner class 폐기_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 폐기를 반환한다`() {
            runBlocking { whenever(registerDisposalUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/disposals")
                .bodyValue(RegisterDisposalRequest(1L, listOf(RegisterDisposalItemRequest(10L, 5, DisposalReason.EXPIRED))))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.disposalId").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("REQUESTED")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/disposals")
                .bodyValue(RegisterDisposalRequest(null, emptyList()))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 폐기_단건_조회 {

        @Test
        fun `존재하면 200을 반환한다`() {
            runBlocking { whenever(getDisposalUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/disposals/{disposalId}", 1L)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getDisposalUseCase.getById(eq(999L))).thenThrow(DisposalNotFoundException()) }

            webTestClient.get().uri("/api/disposals/{disposalId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 폐기_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200을 반환한다`() {
            whenever(getDisposalUseCase.getAll(null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/disposals")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].disposalId").isEqualTo(1)
        }
    }

    @Nested
    inner class 폐기_승인 {

        @Test
        fun `요청이 유효하면 200과 APPROVED 상태를 반환한다`() {
            runBlocking {
                whenever(approveDisposalUseCase.approve(1L)).thenReturn(sampleResult(DisposalStatus.APPROVED))
            }

            webTestClient.patch().uri("/api/disposals/{disposalId}/approve", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("APPROVED")
        }
    }

    @Nested
    inner class 폐기_완료 {

        @Test
        fun `요청이 유효하면 200과 COMPLETED 상태를 반환한다`() {
            runBlocking {
                whenever(completeDisposalUseCase.complete(1L)).thenReturn(sampleResult(DisposalStatus.COMPLETED))
            }

            webTestClient.patch().uri("/api/disposals/{disposalId}/complete", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED")
        }
    }
}
