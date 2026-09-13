package com.dozycoffee.wms.product.adapter.`in`.web

import com.dozycoffee.wms.product.adapter.`in`.web.request.RegisterProductRequest
import com.dozycoffee.wms.product.application.port.`in`.ActivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.DeactivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.RegisterProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.ProductNotFoundException
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

@WebFluxTest(ProductController::class)
class ProductControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @MockitoBean
    private lateinit var activateProductUseCase: ActivateProductUseCase

    @MockitoBean
    private lateinit var deactivateProductUseCase: DeactivateProductUseCase

    @MockitoBean
    private lateinit var getProductUseCase: GetProductUseCase

    private fun sampleResult(productStatus: ProductStatus = ProductStatus.ACTIVE): ProductResult {
        return ProductResult(1L, "PRD-0001", "콜롬비아 원두", ProductCategory.BEAN, "KG", 365, productStatus)
    }

    @Nested
    inner class 상품_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 상품을 반환한다`() {
            runBlocking { whenever(registerProductUseCase.register(any())).thenReturn(sampleResult()) }

            webTestClient.post().uri("/api/products")
                .bodyValue(RegisterProductRequest("PRD-0001", "콜롬비아 원두", ProductCategory.BEAN, "KG", 365))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.productId").isEqualTo(1)
                .jsonPath("$.productStatus").isEqualTo("ACTIVE")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/products")
                .bodyValue(RegisterProductRequest("", "", null, "", null))
                .exchange()
                .expectStatus().isBadRequest
        }
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
    inner class 상품_활성화_비활성화 {

        @Test
        fun `활성화 요청 시 200과 활성화된 상품을 반환한다`() {
            runBlocking { whenever(activateProductUseCase.activate(1L)).thenReturn(sampleResult(ProductStatus.ACTIVE)) }

            webTestClient.patch().uri("/api/products/{productId}/activate", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.productStatus").isEqualTo("ACTIVE")
        }

        @Test
        fun `비활성화 요청 시 200과 비활성화된 상품을 반환한다`() {
            runBlocking { whenever(deactivateProductUseCase.deactivate(1L)).thenReturn(sampleResult(ProductStatus.INACTIVE)) }

            webTestClient.patch().uri("/api/products/{productId}/deactivate", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.productStatus").isEqualTo("INACTIVE")
        }
    }
}
