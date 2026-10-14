package com.dozycoffee.wms.product.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.ProductNotFoundException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WithDozyPrincipal(roles = ["wms:warehouse_manager"])
@WebFluxTest(ProductController::class)
class ProductControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getProductUseCase: GetProductUseCase

    private fun sampleResult(productStatus: ProductStatus = ProductStatus.ACTIVE): ProductResult {
        return ProductResult(1L, "PRD-0001", "콜롬비아 원두", ProductCategory.BEAN, "KG", 365, productStatus)
    }

    @Nested
    inner class 상품_단건_조회 {

        @Test
        fun `존재하면 200과 상품 정보를 반환한다`() {
            runBlocking { whenever(getProductUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/products/{productId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.productId").isEqualTo(1)
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getProductUseCase.getById(eq(999L))).thenThrow(ProductNotFoundException()) }

            webTestClient.get().uri("/api/products/{productId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 상품_목록_조회 {

        @Test
        fun `필터 없이 조회하면 200과 상품 목록을 반환한다`() {
            whenever(getProductUseCase.getAll(null, null)).thenReturn(flowOf(sampleResult()))

            webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].productId").isEqualTo(1)
        }

        @Test
        fun `카테고리와 상태로 필터링하면 200과 필터링된 상품 목록을 반환한다`() {
            whenever(getProductUseCase.getAll(ProductCategory.BEAN, ProductStatus.INACTIVE))
                .thenReturn(flowOf(sampleResult(ProductStatus.INACTIVE)))

            webTestClient.get().uri { it.path("/api/products").queryParam("category", "BEAN").queryParam("status", "INACTIVE").build() }
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].category").isEqualTo("BEAN")
                .jsonPath("$[0].productStatus").isEqualTo("INACTIVE")
        }
    }

    @Nested
    inner class 쓰기_API_제거 {

        @Test
        fun `상품 등록, 활성화, 비활성화, 삭제 경로는 더 이상 없다`() {
            webTestClient.post().uri("/api/products").exchange().expectStatus().isEqualTo(405)
            webTestClient.patch().uri("/api/products/1/activate").exchange().expectStatus().isNotFound
            webTestClient.patch().uri("/api/products/1/deactivate").exchange().expectStatus().isNotFound
            webTestClient.delete().uri("/api/products/1").exchange().expectStatus().isEqualTo(405)
        }
    }
}
