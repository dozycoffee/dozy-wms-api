package com.dozycoffee.wms.location_stock.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.location_stock.application.port.`in`.GetZoneStockSummaryUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.result.ZoneStockSummaryResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WithDozyPrincipal(roles = ["wms:warehouse_manager"])
@WebFluxTest(ZoneStockSummaryController::class)
class ZoneStockSummaryControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getZoneStockSummaryUseCase: GetZoneStockSummaryUseCase

    private fun summary() = ZoneStockSummaryResult(1L, ZoneCode.A, 10L, 180, 90, mapOf(StockQualityStatus.NORMAL to 90))

    @Test
    fun `Zone별 재고 현황을 반환한다`() {
        whenever(getZoneStockSummaryUseCase.getAll(null)).thenReturn(flowOf(summary()))

        webTestClient.get().uri("/api/inventories/zone-summary")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].zoneId").isEqualTo(1)
            .jsonPath("$[0].zoneCode").isEqualTo("A")
            .jsonPath("$[0].warehouseId").isEqualTo(10)
            .jsonPath("$[0].maxCapacity").isEqualTo(180)
            .jsonPath("$[0].usedCapacity").isEqualTo(90)
            .jsonPath("$[0].usageRate").isEqualTo(0.5)
    }

    @Test
    fun `warehouseIds를 지정하면 UseCase에 그대로 전달한다`() {
        whenever(getZoneStockSummaryUseCase.getAll(listOf(10L, 20L))).thenReturn(flowOf(summary()))

        webTestClient.get().uri("/api/inventories/zone-summary?warehouseIds=10,20")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].warehouseId").isEqualTo(10)
    }
}
