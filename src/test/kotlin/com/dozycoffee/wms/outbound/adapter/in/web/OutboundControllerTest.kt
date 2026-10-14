package com.dozycoffee.wms.outbound.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.outbound.application.port.`in`.ApproveOutboundCancelUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.CompleteOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.ReportPickingShortageUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.RequestOutboundCancelUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundInspectingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundPickingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundAllocationResult
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

@WithDozyPrincipal(roles = ["wms:warehouse_manager"])
@WebFluxTest(OutboundController::class)
class OutboundControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var startOutboundPickingUseCase: StartOutboundPickingUseCase

    @MockitoBean
    private lateinit var startOutboundInspectingUseCase: StartOutboundInspectingUseCase

    @MockitoBean
    private lateinit var completeOutboundUseCase: CompleteOutboundUseCase

    @MockitoBean
    private lateinit var reportPickingShortageUseCase: ReportPickingShortageUseCase

    @MockitoBean
    private lateinit var requestOutboundCancelUseCase: RequestOutboundCancelUseCase

    @MockitoBean
    private lateinit var approveOutboundCancelUseCase: ApproveOutboundCancelUseCase

    @MockitoBean
    private lateinit var getOutboundUseCase: GetOutboundUseCase

    private fun sampleResult(status: OutboundStatus = OutboundStatus.REQUESTED): OutboundResult {
        return OutboundResult(1L, 1L, status, 7L, "OMS", "ORDER-1")
    }

    @Nested
    inner class 출고_단건_조회 {

        @Test
        fun `존재하면 200을 반환한다`() {
            runBlocking { whenever(getOutboundUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/outbounds/{outboundId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.reservationId").isEqualTo(7)
                .jsonPath("$.channel").isEqualTo("OMS")
                .jsonPath("$.externalOrderId").isEqualTo("ORDER-1")
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
    @Nested
    inner class 피킹_불가_재할당 {

        @Test
        fun `부족 수량을 보고하면 재할당 뒤의 전체 할당을 반환한다`() {
            runBlocking {
                whenever(reportPickingShortageUseCase.report(any())).thenReturn(
                    listOf(OutboundAllocationResult(5L, 1L, 100L, 56L, "B", null, 4, 4))
                )
            }

            webTestClient.post().uri("/api/outbounds/{outboundId}/reallocation", 1L)
                .bodyValue(mapOf("shortages" to listOf(mapOf("outboundAllocationId" to 5, "shortageQuantity" to 2))))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].outboundAllocationId").isEqualTo(5)
                .jsonPath("$[0].lotNumber").isEqualTo("B")
                .jsonPath("$[0].pickedQuantity").isEqualTo(4)
        }

        @Test
        fun `부족 목록이 비어 있거나 수량이 0 이하이면 400을 반환한다`() {
            webTestClient.post().uri("/api/outbounds/{outboundId}/reallocation", 1L)
                .bodyValue(mapOf("shortages" to emptyList<Any>()))
                .exchange()
                .expectStatus().isBadRequest

            webTestClient.post().uri("/api/outbounds/{outboundId}/reallocation", 1L)
                .bodyValue(mapOf("shortages" to listOf(mapOf("outboundAllocationId" to 5, "shortageQuantity" to 0))))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 출고_취소 {

        @Test
        fun `출고 담당자는 취소를 요청할 수 있다`() {
            runBlocking { whenever(requestOutboundCancelUseCase.requestCancel(1L)).thenReturn(sampleResult(OutboundStatus.CANCEL_REQUESTED)) }

            webTestClient.post().uri("/api/outbounds/{outboundId}/cancel-requests", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("CANCEL_REQUESTED")
        }

        @Test
        @WithDozyPrincipal(roles = ["wms:service"])
        fun `서비스 role도 취소를 요청할 수 있다`() {
            runBlocking { whenever(requestOutboundCancelUseCase.requestCancel(1L)).thenReturn(sampleResult(OutboundStatus.CANCELED)) }

            webTestClient.post().uri("/api/outbounds/{outboundId}/cancel-requests", 1L)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @WithDozyPrincipal(roles = ["wms:viewer"])
        fun `viewer는 취소를 요청할 수 없다`() {
            webTestClient.post().uri("/api/outbounds/{outboundId}/cancel-requests", 1L)
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        @WithDozyPrincipal(roles = ["wms:admin"])
        fun `admin은 취소를 승인할 수 있다`() {
            runBlocking { whenever(approveOutboundCancelUseCase.approveCancel(1L)).thenReturn(sampleResult(OutboundStatus.CANCELED)) }

            webTestClient.post().uri("/api/outbounds/{outboundId}/cancel-approval", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("CANCELED")
        }

        @Test
        fun `출고 담당자는 취소를 승인할 수 없다`() {
            webTestClient.post().uri("/api/outbounds/{outboundId}/cancel-approval", 1L)
                .exchange()
                .expectStatus().isForbidden
        }
    }
}
