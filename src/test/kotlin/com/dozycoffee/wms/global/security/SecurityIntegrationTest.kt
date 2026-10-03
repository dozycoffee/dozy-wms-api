package com.dozycoffee.wms.global.security

import com.dozycoffee.auth.core.PrincipalType
import com.dozycoffee.auth.test.DozyTestTokens
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.MediaType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.flow
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SecurityIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var tokens: DozyTestTokens

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @AfterEach
    fun cleanUp() = runBlocking {
        databaseClient.sql("DELETE FROM product").fetch().awaitRowsUpdated()
        Unit
    }

    private fun get(token: String?): WebTestClient.ResponseSpec {
        val request = webTestClient.get().uri("/api/products")
        return (if (token == null) request else request.header("Authorization", "Bearer $token")).exchange()
    }

    @Test
    fun `토큰 없이 호출하면 401 Problem Details를 반환한다`() {
        get(null)
            .expectStatus().isUnauthorized
            .expectHeader().exists("WWW-Authenticate")
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.code").isEqualTo("UNAUTHENTICATED")
    }

    @Test
    fun `유효한 직원 토큰으로 호출하면 200을 반환한다`() {
        get(tokens.issue(roles = listOf("wms:inventory_viewer"))).expectStatus().isOk
    }

    @Test
    fun `WMS role이 없는 직원 토큰은 403 Problem Details를 반환한다`() {
        get(tokens.issue())
            .expectStatus().isForbidden
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.code").isEqualTo("FORBIDDEN")
    }

    @Test
    fun `조회 전용 role로 상품을 등록하면 403이다`() {
        webTestClient.post().uri("/api/products")
            .header("Authorization", "Bearer ${tokens.issue(roles = listOf("wms:inventory_viewer"))}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "productCode" to "DENIED-001",
                    "productName" to "권한 없는 등록",
                    "category" to "BEAN",
                    "unit" to "KG",
                    "shelfLifeDays" to 30
                )
            )
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `WMS가 받지 않는 realm의 파트너 토큰은 401이다`() {
        get(tokens.issue(type = PrincipalType.PARTNER)).expectStatus().isUnauthorized
    }

    @Test
    fun `다른 서비스를 위한 audience의 토큰은 401이다`() {
        get(tokens.issue(audience = listOf("catalog"))).expectStatus().isUnauthorized
    }

    @Test
    fun `만료된 토큰은 401이다`() {
        get(tokens.issue(expiresAt = Instant.now().minusSeconds(60), issuedAt = Instant.now().minusSeconds(700)))
            .expectStatus().isUnauthorized
    }

    @Test
    fun `믿지 않는 키로 서명한 토큰은 401이다`() {
        get(tokens.issue(signedBy = DozyTestTokens.Key.UNTRUSTED)).expectStatus().isUnauthorized
    }

    @Test
    fun `요청으로 만든 데이터의 감사 주체는 토큰의 principalId다`() {
        val principalId = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")

        webTestClient.post().uri("/api/products")
            .header("Authorization", "Bearer ${tokens.issue(id = principalId, roles = listOf("wms:warehouse_admin"))}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "productCode" to "AUDIT-001",
                    "productName" to "감사 주체 확인용 상품",
                    "category" to "BEAN",
                    "unit" to "KG",
                    "shelfLifeDays" to 30
                )
            )
            .exchange()
            .expectStatus().is2xxSuccessful

        val createdBy: List<String> = runBlocking {
            databaseClient.sql("SELECT created_by FROM product WHERE product_code = 'AUDIT-001'")
                .map { row -> row.get("created_by", String::class.java) ?: "" }
                .flow().toList()
        }
        assertThat(createdBy).containsExactly(principalId.toString())
    }
}
