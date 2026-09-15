package com.dozycoffee.wms.inbound.adapter.`in`.web

import com.dozycoffee.wms.inbound.adapter.`in`.web.request.CompleteInboundRequest
import com.dozycoffee.wms.inbound.adapter.`in`.web.request.LotAssignmentRequest
import com.dozycoffee.wms.inbound.adapter.`in`.web.request.RegisterInboundItemRequest
import com.dozycoffee.wms.inbound.adapter.`in`.web.request.RegisterInboundRequest
import com.dozycoffee.wms.inbound.application.port.`in`.CompleteInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.RegisterInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.StartInboundProcessingUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundResult
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InsufficientZoneCapacityException
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
import java.time.LocalDate

@WebFluxTest(InboundController::class)
class InboundControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerInboundUseCase: RegisterInboundUseCase

    @MockitoBean
    private lateinit var startInboundProcessingUseCase: StartInboundProcessingUseCase

    @MockitoBean
    private lateinit var completeInboundUseCase: CompleteInboundUseCase

    @MockitoBean
    private lateinit var getInboundUseCase: GetInboundUseCase

    private fun sampleResult(status: InboundStatus = InboundStatus.WAITING): InboundResult {
        return InboundResult(1L, 1L, LocalDate.of(2026, 1, 1), status)
    }

    @Nested
    inner class 입고_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 입고를 반환한다`() {
            runBlocking { whenever(registerInboundUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/inbounds")
                .bodyValue(
                    RegisterInboundRequest(
                        1L,
                        LocalDate.of(2026, 1, 1),
                        listOf(RegisterInboundItemRequest(100L, 30))
                    )
                )
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.inboundId").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("WAITING")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/inbounds")
                .bodyValue(RegisterInboundRequest(null, null, emptyList()))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `Zone 잔여 capacity가 부족하면 409를 반환한다`() {
            runBlocking {
                whenever(registerInboundUseCase.register(any())).thenThrow(InsufficientZoneCapacityException())
            }

            webTestClient.post().uri("/api/inbounds")
                .bodyValue(
                    RegisterInboundRequest(
                        1L,
                        LocalDate.of(2026, 1, 1),
                        listOf(RegisterInboundItemRequest(100L, 9999))
                    )
                )
                .exchange()
                .expectStatus().isEqualTo(409)
        }
    }

    @Nested
    inner class 입고_단건_조회 {

        @Test
        fun `존재하면 200을 반환한다`() {
            runBlocking { whenever(getInboundUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/inbounds/{inboundId}", 1L)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getInboundUseCase.getById(eq(999L))).thenThrow(InboundNotFoundException()) }

            webTestClient.get().uri("/api/inbounds/{inboundId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 입고_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200을 반환한다`() {
            whenever(getInboundUseCase.getAll(null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/inbounds")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].inboundId").isEqualTo(1)
        }
    }

    @Nested
    inner class 입고_처리_시작 {

        @Test
        fun `요청이 유효하면 200과 PROCESSING 상태를 반환한다`() {
            runBlocking {
                whenever(startInboundProcessingUseCase.startProcessing(1L)).thenReturn(sampleResult(InboundStatus.PROCESSING))
            }

            webTestClient.patch().uri("/api/inbounds/{inboundId}/processing", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("PROCESSING")
        }
    }

    @Nested
    inner class 입고_완료 {

        @Test
        fun `요청이 유효하면 200과 COMPLETED 상태를 반환한다`() {
            runBlocking {
                whenever(completeInboundUseCase.complete(any())).thenReturn(sampleResult(InboundStatus.COMPLETED))
            }

            webTestClient.patch().uri("/api/inbounds/{inboundId}/complete", 1L)
                .bodyValue(CompleteInboundRequest(listOf(LotAssignmentRequest(1L, "LOT-1", null, null))))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED")
        }
    }
}
