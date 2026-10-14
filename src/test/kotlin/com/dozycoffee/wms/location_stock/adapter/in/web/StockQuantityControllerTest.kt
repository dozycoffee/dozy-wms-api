package com.dozycoffee.wms.location_stock.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.location_stock.application.port.`in`.GetWarehouseStockQuantitiesUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.result.WarehouseStockQuantityResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WithDozyPrincipal(roles = ["wms:service"])
@WebFluxTest(StockQuantityController::class)
class StockQuantityControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getWarehouseStockQuantitiesUseCase: GetWarehouseStockQuantitiesUseCase

    @Test
    fun `서비스 role로 조회하면 inventory 계약 형식으로 수량을 반환한다`() {
        runBlocking {
            whenever(getWarehouseStockQuantitiesUseCase.getQuantities(10L, null, null)).thenReturn(
                listOf(
                    WarehouseStockQuantityResult(1L, "L-1", StockQualityStatus.NORMAL, 30),
                    WarehouseStockQuantityResult(1L, "L-1", StockQualityStatus.DISPOSAL_SCHEDULED, 5)
                )
            )
        }

        webTestClient.get().uri("/api/v1/stock-quantities?warehouseId=10")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.items.length()").isEqualTo(2)
            .jsonPath("$.items[0].productId").isEqualTo(1)
            .jsonPath("$.items[0].lotNumber").isEqualTo("L-1")
            .jsonPath("$.items[0].qualityStatus").isEqualTo("NORMAL")
            .jsonPath("$.items[0].quantity").isEqualTo(30)
            .jsonPath("$.items[1].qualityStatus").isEqualTo("DISPOSAL_SCHEDULED")
    }

    @Test
    fun `상품과 Lot 번호 조건을 그대로 전달한다`() {
        runBlocking {
            whenever(getWarehouseStockQuantitiesUseCase.getQuantities(10L, 1L, "L-1")).thenReturn(
                listOf(WarehouseStockQuantityResult(1L, "L-1", StockQualityStatus.NORMAL, 30))
            )
        }

        webTestClient.get().uri("/api/v1/stock-quantities?warehouseId=10&productId=1&lotNumber=L-1")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.items.length()").isEqualTo(1)
    }

    @Test
    fun `재고가 없으면 빈 items를 반환한다`() {
        runBlocking { whenever(getWarehouseStockQuantitiesUseCase.getQuantities(10L, null, null)).thenReturn(emptyList()) }

        webTestClient.get().uri("/api/v1/stock-quantities?warehouseId=10")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.items.length()").isEqualTo(0)
    }

    @Test
    fun `창고가 없으면 404다`() {
        runBlocking {
            whenever(getWarehouseStockQuantitiesUseCase.getQuantities(99L, null, null)).thenThrow(WarehouseNotFoundException())
        }

        webTestClient.get().uri("/api/v1/stock-quantities?warehouseId=99")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `warehouseId가 없으면 400이다`() {
        webTestClient.get().uri("/api/v1/stock-quantities").exchange().expectStatus().isBadRequest
    }

    @Test
    @WithDozyPrincipal(roles = ["wms:admin"])
    fun `admin도 조회할 수 있다`() {
        runBlocking { whenever(getWarehouseStockQuantitiesUseCase.getQuantities(10L, null, null)).thenReturn(emptyList()) }

        webTestClient.get().uri("/api/v1/stock-quantities?warehouseId=10").exchange().expectStatus().isOk
    }

    @Test
    @WithDozyPrincipal(roles = ["wms:warehouse_manager", "wms:viewer"])
    fun `서비스 role이나 관리자가 아니면 403이다`() {
        webTestClient.get().uri("/api/v1/stock-quantities?warehouseId=10").exchange().expectStatus().isForbidden
    }
}
