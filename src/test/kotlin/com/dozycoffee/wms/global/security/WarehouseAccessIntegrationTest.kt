package com.dozycoffee.wms.global.security

import com.dozycoffee.auth.test.DozyTestTokens
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.MediaType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class WarehouseAccessIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var tokens: DozyTestTokens

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    private val manager: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")
    private val unassigned: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d60")

    private var warehouseA: Long = 0
    private var warehouseB: Long = 0
    private var inboundA: Long = 0
    private var inboundB: Long = 0

    @BeforeEach
    fun setUp() = runBlocking {
        warehouseA = insertWarehouse("창고A")
        warehouseB = insertWarehouse("창고B")
        inboundA = insertInbound(warehouseA)
        inboundB = insertInbound(warehouseB)
        execute(
            "INSERT INTO warehouse_member (warehouse_id, principal_id, created_at, created_by, updated_at, updated_by) " +
                "VALUES ($warehouseA, '$manager', NOW(6), 'test', NOW(6), 'test')"
        )
    }

    @AfterEach
    fun cleanUp() = runBlocking {
        execute("DELETE FROM warehouse_member")
        execute("DELETE FROM inbound")
        execute("DELETE FROM warehouse")
    }

    private suspend fun execute(sql: String) {
        databaseClient.sql(sql).fetch().awaitRowsUpdated()
    }

    private suspend fun insertWarehouse(name: String): Long {
        execute(
            "INSERT INTO warehouse (warehouse_name, address, latitude, longitude, warehouse_status, " +
                "created_at, created_by, updated_at, updated_by) VALUES ('$name', '주소', 37.5, 127.0, " +
                "'AVAILABLE', NOW(6), 'test', NOW(6), 'test')"
        )
        return lastInsertId()
    }

    private suspend fun insertInbound(warehouseId: Long): Long {
        execute(
            "INSERT INTO inbound (warehouse_id, expected_arrival_date, status, created_at, created_by, updated_at, updated_by) " +
                "VALUES ($warehouseId, '2026-01-01', 'WAITING', NOW(6), 'test', NOW(6), 'test')"
        )
        return lastInsertId()
    }

    private suspend fun lastInsertId(): Long =
        databaseClient.sql("SELECT LAST_INSERT_ID() AS id")
            .map { row -> requireNotNull(row.get("id", java.lang.Long::class.java)).toLong() }
            .awaitOne()

    private fun bearer(principalId: UUID, vararg roles: String): String =
        "Bearer ${tokens.issue(id = principalId, roles = roles.toList())}"

    @Test
    fun `배정된 창고의 입고만 목록에 보인다`() {
        webTestClient.get().uri("/api/inbounds")
            .header("Authorization", bearer(manager, "wms:inbound_manager"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].inboundId").isEqualTo(inboundA)
    }

    @Test
    fun `배정되지 않은 창고의 입고는 단건 조회가 403이다`() {
        webTestClient.get().uri("/api/inbounds/$inboundB")
            .header("Authorization", bearer(manager, "wms:inbound_manager"))
            .exchange()
            .expectStatus().isForbidden
            .expectBody().jsonPath("$.errorCode").isEqualTo("COMMON_WAREHOUSE_ACCESS_DENIED")
    }

    @Test
    fun `배정된 창고의 입고는 단건 조회할 수 있다`() {
        webTestClient.get().uri("/api/inbounds/$inboundA")
            .header("Authorization", bearer(manager, "wms:inbound_manager"))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `배정되지 않은 창고에는 입고를 등록할 수 없다`() {
        webTestClient.post().uri("/api/inbounds")
            .header("Authorization", bearer(manager, "wms:inbound_manager"))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "warehouseId" to warehouseB,
                    "expectedArrivalDate" to "2026-01-01",
                    "items" to listOf(mapOf("productId" to 1, "expectedQuantity" to 10))
                )
            )
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `배정이 없는 담당자는 목록이 비어 있고 단건 조회가 403이다`() {
        webTestClient.get().uri("/api/inbounds")
            .header("Authorization", bearer(unassigned, "wms:inbound_manager"))
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.length()").isEqualTo(0)

        webTestClient.get().uri("/api/inbounds/$inboundA")
            .header("Authorization", bearer(unassigned, "wms:inbound_manager"))
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `warehouse_admin은 배정과 무관하게 모든 창고를 본다`() {
        webTestClient.get().uri("/api/inbounds")
            .header("Authorization", bearer(unassigned, "wms:warehouse_admin"))
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.length()").isEqualTo(2)
    }

    @Test
    fun `warehouse_admin이 배정한 창고가 해당 담당자의 접근 범위에 반영된다`() {
        webTestClient.post().uri("/api/warehouses/$warehouseB/members")
            .header("Authorization", bearer(unassigned, "wms:warehouse_admin"))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("principalId" to manager.toString()))
            .exchange()
            .expectStatus().isCreated

        webTestClient.get().uri("/api/inbounds")
            .header("Authorization", bearer(manager, "wms:inbound_manager"))
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.length()").isEqualTo(2)
    }
}
