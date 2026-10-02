package com.dozycoffee.wms.global.config

import com.dozycoffee.auth.test.DozyTestTokens
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

// CORS는 인증 필터 체인 안에서 처리되는 cross-cutting 동작이라 @WebFluxTest 슬라이스로는 실제 동작과 다르게
// 재현된다 — 실제 서버(bootRun)와 같은 구성인 전체 컨텍스트 + 임베디드 서버로 검증한다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = ["wms.cors.allowed-origins=http://localhost:5173"])
class CorsConfigTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var tokens: DozyTestTokens

    private fun bearer(): String = "Bearer ${tokens.issue()}"

    @Test
    fun `허용된 origin으로 요청하면 Access-Control-Allow-Origin 헤더를 붙여 응답한다`() {
        webTestClient.get().uri("/api/products")
            .header("Origin", "http://localhost:5173")
            .header("Authorization", bearer())
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
    }

    @Test
    fun `허용된 origin의 인증 실패 응답에도 CORS 헤더가 붙어 브라우저가 401을 읽을 수 있다`() {
        webTestClient.get().uri("/api/products")
            .header("Origin", "http://localhost:5173")
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
    }

    @Test
    fun `허용되지 않은 origin으로 요청하면 403을 반환한다`() {
        webTestClient.get().uri("/api/products")
            .header("Origin", "http://evil.example.com")
            .header("Authorization", bearer())
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `허용된 origin의 preflight 요청은 토큰 없이도 200과 함께 허용 헤더를 반환한다`() {
        webTestClient.options().uri("/api/products")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "authorization")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
    }

    @Test
    fun `허용되지 않은 origin의 preflight 요청은 403을 반환한다`() {
        webTestClient.options().uri("/api/products")
            .header("Origin", "http://evil.example.com")
            .header("Access-Control-Request-Method", "GET")
            .exchange()
            .expectStatus().isForbidden
    }
}
