package com.dozycoffee.wms.inventory.adapter.`in`.web

import com.dozycoffee.wms.inventory.adapter.`in`.web.request.RegisterLotRequest
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetOutboundRecommendationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotDetailResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotDistributionResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.OutboundRecommendationResult
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.exception.LotNotFoundException
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
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
import java.time.LocalDateTime

@WebFluxTest(LotController::class)
class LotControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerLotUseCase: RegisterLotUseCase

    @MockitoBean
    private lateinit var getLotUseCase: GetLotUseCase

    @MockitoBean
    private lateinit var getOutboundRecommendationUseCase: GetOutboundRecommendationUseCase

    private fun sampleResult(): LotResult {
        return LotResult(1L, "LOT-20260101-001", 1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LotStatus.NORMAL)
    }

    private fun sampleDetailResult(): LotDetailResult {
        return LotDetailResult(sampleResult(), listOf(LotDistributionResult(10L, 100L, ZoneCode.A, 20)))
    }

    @Nested
    inner class Lot_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 Lot을 반환한다`() {
            runBlocking { whenever(registerLotUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/lots")
                .bodyValue(
                    RegisterLotRequest("LOT-20260101-001", 1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
                )
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.lotId").isEqualTo(1)
                .jsonPath("$.lotStatus").isEqualTo("NORMAL")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/lots")
                .bodyValue(RegisterLotRequest("", null, null, null))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class Lot_단건_조회 {

        @Test
        fun `존재하면 200과 Lot 정보·Zone Location별 분포를 반환한다`() {
            runBlocking { whenever(getLotUseCase.getDetailById(1L)).thenReturn(sampleDetailResult()) }

            webTestClient.get().uri("/api/lots/{lotId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.lot.lotId").isEqualTo(1)
                .jsonPath("$.distribution[0].locationId").isEqualTo(10)
                .jsonPath("$.distribution[0].zoneCode").isEqualTo("A")
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getLotUseCase.getDetailById(eq(999L))).thenThrow(LotNotFoundException()) }

            webTestClient.get().uri("/api/lots/{lotId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class Lot_목록_조회 {

        @Test
        fun `상품 ID로 조회하면 200과 Lot 목록을 반환한다`() {
            whenever(getLotUseCase.getAllByProduct(1L)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri { it.path("/api/lots").queryParam("productId", 1L).build() }
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].lotId").isEqualTo(1)
        }
    }

    @Nested
    inner class 우선_출고_권고_조회 {

        @Test
        fun `우선 출고 권고 목록을 반환한다`() {
            val recommendation = OutboundRecommendationResult(
                lotId = 1L,
                lotNumber = "LOT-20260101-001",
                productId = 1L,
                productName = "콜롬비아 원두",
                expirationDate = LocalDate.of(2026, 10, 1),
                availableQuantity = 15,
                recommendedAt = LocalDateTime.of(2026, 9, 19, 1, 0)
            )
            whenever(getOutboundRecommendationUseCase.getAll()).thenReturn(flowOf(recommendation))

            webTestClient.get().uri("/api/lots/outbound-recommendations")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].lotId").isEqualTo(1)
                .jsonPath("$[0].productName").isEqualTo("콜롬비아 원두")
                .jsonPath("$[0].availableQuantity").isEqualTo(15)
        }
    }
}
