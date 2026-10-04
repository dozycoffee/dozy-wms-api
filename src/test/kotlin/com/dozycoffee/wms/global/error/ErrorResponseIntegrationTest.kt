package com.dozycoffee.wms.global.error

import com.dozycoffee.auth.test.DozyTestTokens
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

/** 실제 애플리케이션 컨텍스트(Security 체인, Boot 기본 에러 핸들러 포함)에서도 에러 응답이 같은 형식인지 검증한다 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ErrorResponseIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var tokens: DozyTestTokens

    private fun authorized(): String = "Bearer ${tokens.issue(roles = listOf("wms:warehouse_admin"))}"

    @Test
    fun `존재하지 않는 리소스는 도메인 ErrorCode의 Problem Details로 응답한다`() {
        webTestClient.get().uri("/api/products/999999")
            .header("Authorization", authorized())
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectHeader().exists("X-Trace-Id")
            .expectBody()
            .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND")
            .jsonPath("$.instance").isEqualTo("/api/products/999999")
            .jsonPath("$.traceId").isNotEmpty
    }

    @Test
    fun `매핑되지 않은 경로도 Problem Details 404로 응답한다`() {
        webTestClient.get().uri("/api/no-such-resource")
            .header("Authorization", authorized())
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("NOT_FOUND")
            .jsonPath("$.traceId").isNotEmpty
    }

    @Test
    fun `깨진 JSON 요청은 Problem Details 400으로 응답한다`() {
        webTestClient.post().uri("/api/products")
            .header("Authorization", authorized())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{not-json")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
    }

    @Test
    fun `요청 본문 검증 실패는 필드별 errors를 포함한다`() {
        webTestClient.post().uri("/api/products")
            .header("Authorization", authorized())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"productCode": "", "productName": "", "category": "BEAN", "unit": "KG"}""")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
            .jsonPath("$.errors[?(@.field == 'productCode')]").exists()
    }
}
