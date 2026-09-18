package com.dozycoffee.wms.inventory.adapter.`in`.web

import com.dozycoffee.wms.inventory.adapter.`in`.web.request.RegisterInventoryRequest
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetZoneInventorySummaryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.InventorySortBy
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDisposalScheduledUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryDetailResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.ZoneInventorySummaryResult
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotFoundException
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

@WebFluxTest(InventoryController::class)
class InventoryControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerInventoryUseCase: RegisterInventoryUseCase

    @MockitoBean
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @MockitoBean
    private lateinit var getZoneInventorySummaryUseCase: GetZoneInventorySummaryUseCase

    @MockitoBean
    private lateinit var markInventoryDefectiveUseCase: MarkInventoryDefectiveUseCase

    @MockitoBean
    private lateinit var markInventoryDisposalScheduledUseCase: MarkInventoryDisposalScheduledUseCase

    private fun sampleResult(qualityStatus: QualityStatus = QualityStatus.NORMAL): InventoryResult {
        return InventoryResult(1L, 1L, 1L, 1L, 20, 0, 20, qualityStatus)
    }

    private fun sampleDetailResult(): InventoryDetailResult {
        val lot = LotResult(1L, "LOT-20260101-001", 1L, null, null, LotStatus.NORMAL)
        return InventoryDetailResult(sampleResult(), lot, emptyList())
    }

    @Nested
    inner class 재고_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 재고를 반환한다`() {
            runBlocking { whenever(registerInventoryUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/inventories")
                .bodyValue(RegisterInventoryRequest(1L, 1L, 20, 100L))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.inventoryId").isEqualTo(1)
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/inventories")
                .bodyValue(RegisterInventoryRequest(null, null, null, null))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 재고_단건_조회 {

        @Test
        fun `존재하면 200과 재고·Lot·최근 이력을 함께 반환한다`() {
            runBlocking { whenever(getInventoryUseCase.getDetailById(1L)).thenReturn(sampleDetailResult()) }

            webTestClient.get().uri("/api/inventories/{inventoryId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.inventory.inventoryId").isEqualTo(1)
                .jsonPath("$.lot.lotId").isEqualTo(1)
                .jsonPath("$.recentHistories").isArray
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getInventoryUseCase.getDetailById(eq(999L))).thenThrow(InventoryNotFoundException()) }

            webTestClient.get().uri("/api/inventories/{inventoryId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class Zone별_재고_현황_조회 {

        @Test
        fun `조회하면 200과 Zone별 Capacity·품질상태별 수량을 반환한다`() {
            val summary = ZoneInventorySummaryResult(1L, ZoneCode.A, 180, 90, mapOf(QualityStatus.NORMAL to 90))
            whenever(getZoneInventorySummaryUseCase.getAll()).thenReturn(flowOf(summary))

            webTestClient.get().uri("/api/inventories/zone-summary")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].zoneId").isEqualTo(1)
                .jsonPath("$[0].zoneCode").isEqualTo("A")
                .jsonPath("$[0].maxCapacity").isEqualTo(180)
                .jsonPath("$[0].usedCapacity").isEqualTo(90)
                .jsonPath("$[0].usageRate").isEqualTo(0.5)
        }
    }

    @Nested
    inner class 재고_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200과 재고 목록을 반환한다`() {
            whenever(getInventoryUseCase.getAll(null, null, null, null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/inventories")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].inventoryId").isEqualTo(1)
        }

        @Test
        fun `정렬 기준을 지정하면 UseCase에 그대로 전달한다`() {
            whenever(getInventoryUseCase.getAll(null, null, null, InventorySortBy.EXPIRATION_DATE))
                .thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/inventories?sortBy=EXPIRATION_DATE")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].inventoryId").isEqualTo(1)
        }
    }

    @Nested
    inner class 재고_불량_및_폐기예정_처리 {

        @Test
        fun `불량 처리 요청 시 200과 갱신된 재고를 반환한다`() {
            runBlocking {
                whenever(markInventoryDefectiveUseCase.markDefective(1L)).thenReturn(sampleResult(QualityStatus.DEFECTIVE))
            }

            webTestClient.patch().uri("/api/inventories/{inventoryId}/mark-defective", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.qualityStatus").isEqualTo("DEFECTIVE")
        }

        @Test
        fun `폐기예정 처리 요청 시 200과 갱신된 재고를 반환한다`() {
            runBlocking {
                whenever(markInventoryDisposalScheduledUseCase.markDisposalScheduled(1L))
                    .thenReturn(sampleResult(QualityStatus.DISPOSAL_SCHEDULED))
            }

            webTestClient.patch().uri("/api/inventories/{inventoryId}/mark-disposal-scheduled", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.qualityStatus").isEqualTo("DISPOSAL_SCHEDULED")
        }
    }
}
