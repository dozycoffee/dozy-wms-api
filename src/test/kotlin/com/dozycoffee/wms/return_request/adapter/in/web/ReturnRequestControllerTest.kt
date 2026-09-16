package com.dozycoffee.wms.return_request.adapter.`in`.web

import com.dozycoffee.wms.return_request.adapter.`in`.web.request.CompleteReturnRequestRequest
import com.dozycoffee.wms.return_request.adapter.`in`.web.request.RegisterReturnItemRequest
import com.dozycoffee.wms.return_request.adapter.`in`.web.request.RegisterReturnRequestRequest
import com.dozycoffee.wms.return_request.adapter.`in`.web.request.ReturnItemLotAssignmentRequest
import com.dozycoffee.wms.return_request.application.port.`in`.CompleteReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.GetReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.RegisterReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.StartReturnInspectingUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnRequestResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.exception.ReturnRequestNotFoundException
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

@WebFluxTest(ReturnRequestController::class)
class ReturnRequestControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerReturnRequestUseCase: RegisterReturnRequestUseCase

    @MockitoBean
    private lateinit var startReturnInspectingUseCase: StartReturnInspectingUseCase

    @MockitoBean
    private lateinit var completeReturnRequestUseCase: CompleteReturnRequestUseCase

    @MockitoBean
    private lateinit var getReturnRequestUseCase: GetReturnRequestUseCase

    private fun sampleResult(status: ReturnRequestStatus = ReturnRequestStatus.RECEIVED): ReturnRequestResult {
        return ReturnRequestResult(1L, 1L, status)
    }

    @Nested
    inner class 반품_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 반품을 반환한다`() {
            runBlocking { whenever(registerReturnRequestUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/return-requests")
                .bodyValue(RegisterReturnRequestRequest(1L, listOf(RegisterReturnItemRequest(10L, 5))))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.returnRequestId").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("RECEIVED")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/return-requests")
                .bodyValue(RegisterReturnRequestRequest(null, emptyList()))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 반품_단건_조회 {

        @Test
        fun `존재하면 200을 반환한다`() {
            runBlocking { whenever(getReturnRequestUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/return-requests/{returnRequestId}", 1L)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getReturnRequestUseCase.getById(eq(999L))).thenThrow(ReturnRequestNotFoundException()) }

            webTestClient.get().uri("/api/return-requests/{returnRequestId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 반품_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200을 반환한다`() {
            whenever(getReturnRequestUseCase.getAll(null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/return-requests")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].returnRequestId").isEqualTo(1)
        }
    }

    @Nested
    inner class 검수_시작 {

        @Test
        fun `요청이 유효하면 200과 INSPECTING 상태를 반환한다`() {
            runBlocking {
                whenever(startReturnInspectingUseCase.startInspecting(1L)).thenReturn(sampleResult(ReturnRequestStatus.INSPECTING))
            }

            webTestClient.patch().uri("/api/return-requests/{returnRequestId}/start-inspecting", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("INSPECTING")
        }
    }

    @Nested
    inner class 반품_완료 {

        @Test
        fun `요청이 유효하면 200과 COMPLETED 상태를 반환한다`() {
            runBlocking {
                whenever(completeReturnRequestUseCase.complete(any())).thenReturn(sampleResult(ReturnRequestStatus.COMPLETED))
            }

            webTestClient.patch().uri("/api/return-requests/{returnRequestId}/complete", 1L)
                .bodyValue(CompleteReturnRequestRequest(listOf(ReturnItemLotAssignmentRequest(1L, "LOT-1", null, null))))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED")
        }
    }
}
