package com.dozycoffee.wms.outbound.adapter.`in`.web

import com.dozycoffee.wms.outbound.adapter.`in`.web.request.RegisterOutboundItemRequest
import com.dozycoffee.wms.outbound.adapter.`in`.web.request.RegisterOutboundRequest
import com.dozycoffee.wms.outbound.application.port.`in`.CompleteOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.RegisterOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundInspectingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundPickingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
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

@WebFluxTest(OutboundController::class)
class OutboundControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerOutboundUseCase: RegisterOutboundUseCase

    @MockitoBean
    private lateinit var startOutboundPickingUseCase: StartOutboundPickingUseCase

    @MockitoBean
    private lateinit var startOutboundInspectingUseCase: StartOutboundInspectingUseCase

    @MockitoBean
    private lateinit var completeOutboundUseCase: CompleteOutboundUseCase

    @MockitoBean
    private lateinit var getOutboundUseCase: GetOutboundUseCase

    private fun sampleResult(status: OutboundStatus = OutboundStatus.REQUESTED): OutboundResult {
        return OutboundResult(1L, 1L, status)
    }

    @Nested
    inner class 출고_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 출고를 반환한다`() {
            runBlocking { whenever(registerOutboundUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/outbounds")
                .bodyValue(RegisterOutboundRequest(1L, listOf(RegisterOutboundItemRequest(100L, 15))))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.outboundId").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("REQUESTED")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/outbounds")
                .bodyValue(RegisterOutboundRequest(null, emptyList()))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 출고_단건_조회 {

        @Test
        fun `존재하면 200을 반환한다`() {
            runBlocking { whenever(getOutboundUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/outbounds/{outboundId}", 1L)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getOutboundUseCase.getById(eq(999L))).thenThrow(OutboundNotFoundException()) }

            webTestClient.get().uri("/api/outbounds/{outboundId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 출고_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200을 반환한다`() {
            whenever(getOutboundUseCase.getAll(null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/outbounds")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].outboundId").isEqualTo(1)
        }
    }

    @Nested
    inner class 피킹_시작 {

        @Test
        fun `요청이 유효하면 200과 PICKING 상태를 반환한다`() {
            runBlocking {
                whenever(startOutboundPickingUseCase.startPicking(1L)).thenReturn(sampleResult(OutboundStatus.PICKING))
            }

            webTestClient.patch().uri("/api/outbounds/{outboundId}/picking", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("PICKING")
        }
    }

    @Nested
    inner class 검수_시작 {

        @Test
        fun `요청이 유효하면 200과 INSPECTING 상태를 반환한다`() {
            runBlocking {
                whenever(startOutboundInspectingUseCase.startInspecting(1L)).thenReturn(sampleResult(OutboundStatus.INSPECTING))
            }

            webTestClient.patch().uri("/api/outbounds/{outboundId}/inspecting", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("INSPECTING")
        }
    }

    @Nested
    inner class 출고_완료 {

        @Test
        fun `요청이 유효하면 200과 COMPLETED 상태를 반환한다`() {
            runBlocking {
                whenever(completeOutboundUseCase.complete(1L)).thenReturn(sampleResult(OutboundStatus.COMPLETED))
            }

            webTestClient.patch().uri("/api/outbounds/{outboundId}/complete", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED")
        }
    }
}
