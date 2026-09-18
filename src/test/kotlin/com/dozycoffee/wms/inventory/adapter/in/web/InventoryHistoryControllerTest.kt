package com.dozycoffee.wms.inventory.adapter.`in`.web

import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryHistoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryHistoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate
import java.time.LocalDateTime

@WebFluxTest(InventoryHistoryController::class)
class InventoryHistoryControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getInventoryHistoryUseCase: GetInventoryHistoryUseCase

    private fun sampleResult(): InventoryHistoryResult {
        return InventoryHistoryResult(1L, 1L, InventoryHistoryType.INBOUND, 10, 100L, LocalDateTime.now())
    }

    @Nested
    inner class 재고_이력_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200과 이력 목록을 반환한다`() {
            whenever(getInventoryHistoryUseCase.getAll(null, null, null, null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/inventory-histories")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].inventoryHistoryId").isEqualTo(1)
                .jsonPath("$[0].historyType").isEqualTo("INBOUND")
        }

        @Test
        fun `inventoryId, 유형, 기간으로 필터링해 조회한다`() {
            val from = LocalDate.of(2026, 9, 1)
            val to = LocalDate.of(2026, 9, 16)
            whenever(getInventoryHistoryUseCase.getAll(eq(1L), eq(InventoryHistoryType.OUTBOUND), eq(from), eq(to)))
                .thenReturn(flowOf(sampleResult()))

            webTestClient.get()
                .uri("/api/inventory-histories?inventoryId=1&historyType=OUTBOUND&from=2026-09-01&to=2026-09-16")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].inventoryHistoryId").isEqualTo(1)
        }
    }
}
