package com.dozycoffee.wms.location_stock.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.location_stock.application.port.`in`.ListLocationStocksUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.query.LocationStockQuery
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockDetailResult
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
@WebFluxTest(LocationStockQueryController::class)
class LocationStockQueryControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var listLocationStocksUseCase: ListLocationStocksUseCase

    private fun detail() = LocationStockDetailResult(
        1L, 10L, 5L, ZoneCode.A, 7L, "A-01", 100L, "LOT-1", StockQualityStatus.NORMAL, 30, 10, null
    )

    @Test
    fun `Location별 재고를 items로 감싸 반환한다`() {
        whenever(listLocationStocksUseCase.getAll(LocationStockQuery())).thenReturn(flowOf(detail()))

        webTestClient.get().uri("/api/location-stocks")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.items[0].locationStockId").isEqualTo(1)
            .jsonPath("$.items[0].warehouseId").isEqualTo(10)
            .jsonPath("$.items[0].zoneId").isEqualTo(5)
            .jsonPath("$.items[0].zoneCode").isEqualTo("A")
            .jsonPath("$.items[0].locationId").isEqualTo(7)
            .jsonPath("$.items[0].locationCode").isEqualTo("A-01")
            .jsonPath("$.items[0].productId").isEqualTo(100)
            .jsonPath("$.items[0].lotNumber").isEqualTo("LOT-1")
            .jsonPath("$.items[0].qualityStatus").isEqualTo("NORMAL")
            .jsonPath("$.items[0].quantity").isEqualTo(30)
            .jsonPath("$.items[0].heldQuantity").isEqualTo(10)
            .jsonPath("$.items[0].availableQuantity").isEqualTo(20)
    }

    @Test
    fun `조회 조건을 UseCase에 그대로 전달한다`() {
        val query = LocationStockQuery(listOf(10L, 20L), 5L, 7L, 100L, StockQualityStatus.DEFECTIVE, true)
        whenever(listLocationStocksUseCase.getAll(query)).thenReturn(flowOf(detail()))

        webTestClient.get()
            .uri("/api/location-stocks?warehouseIds=10,20&zoneId=5&locationId=7&productId=100&qualityStatus=DEFECTIVE&includeEmpty=true")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.items[0].locationStockId").isEqualTo(1)
    }

    @Test
    fun `결과가 없으면 빈 items를 반환한다`() {
        whenever(listLocationStocksUseCase.getAll(LocationStockQuery())).thenReturn(flowOf())

        webTestClient.get().uri("/api/location-stocks")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.items").isEmpty
    }

    @Test
    fun `알 수 없는 품질 상태는 400이다`() {
        webTestClient.get().uri("/api/location-stocks?qualityStatus=UNKNOWN")
            .exchange()
            .expectStatus().isBadRequest
    }
}
