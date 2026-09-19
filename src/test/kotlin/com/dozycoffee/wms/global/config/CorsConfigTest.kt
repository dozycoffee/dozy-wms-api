package com.dozycoffee.wms.global.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

// CORS는 RequestMappingHandlerMapping이 WebFluxConfigurer.addCorsMappings()로 등록된 설정을 직접
// 소비하는 cross-cutting 동작이라 @WebFluxTest 슬라이스로는 실제 동작과 다르게 재현된다(슬라이스가
// 전체 WebFlux 구성을 그대로 부트스트랩하지 않아 정상 origin도 403으로 거부됨) — 실제 서버(bootRun)
// 동작과 일치하는 걸 확인하려고 전체 컨텍스트 + 임베디드 서버로 검증한다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = ["wms.cors.allowed-origins=http://localhost:5173"])
class CorsConfigTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `허용된 origin으로 요청하면 Access-Control-Allow-Origin 헤더를 붙여 응답한다`() {
        webTestClient.get().uri("/api/products")
            .header("Origin", "http://localhost:5173")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
    }

    @Test
    fun `허용되지 않은 origin으로 요청하면 403을 반환한다`() {
        webTestClient.get().uri("/api/products")
            .header("Origin", "http://evil.example.com")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `허용된 origin의 preflight 요청은 200과 함께 허용 헤더를 반환한다`() {
        webTestClient.options().uri("/api/products")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "GET")
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
