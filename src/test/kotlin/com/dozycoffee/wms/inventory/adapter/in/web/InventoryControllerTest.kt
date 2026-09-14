package com.dozycoffee.wms.inventory.adapter.`in`.web

import com.dozycoffee.wms.inventory.adapter.`in`.web.request.RegisterInventoryRequest
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDisposalScheduledUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotFoundException
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
    private lateinit var markInventoryDefectiveUseCase: MarkInventoryDefectiveUseCase

    @MockitoBean
    private lateinit var markInventoryDisposalScheduledUseCase: MarkInventoryDisposalScheduledUseCase

    private fun sampleResult(qualityStatus: QualityStatus = QualityStatus.NORMAL): InventoryResult {
        return InventoryResult(1L, 1L, 1L, 1L, 20, 0, 20, qualityStatus)
    }

    @Nested
    inner class 재고_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 재고를 반환한다`() {
            runBlocking { whenever(registerInventoryUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/inventories")
                .bodyValue(RegisterInventoryRequest(1L, 1L, 20))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.inventoryId").isEqualTo(1)
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/inventories")
                .bodyValue(RegisterInventoryRequest(null, null, null))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 재고_단건_조회 {

        @Test
        fun `존재하면 200과 재고 정보를 반환한다`() {
            runBlocking { whenever(getInventoryUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/inventories/{inventoryId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.inventoryId").isEqualTo(1)
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getInventoryUseCase.getById(eq(999L))).thenThrow(InventoryNotFoundException()) }

            webTestClient.get().uri("/api/inventories/{inventoryId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 재고_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200과 재고 목록을 반환한다`() {
            whenever(getInventoryUseCase.getAll(null, null, null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/inventories")
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
