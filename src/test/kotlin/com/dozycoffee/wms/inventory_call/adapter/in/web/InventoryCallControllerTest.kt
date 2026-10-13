package com.dozycoffee.wms.inventory_call.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.inventory_call.application.port.`in`.DiscardInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.GetInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.RequeueInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryCallResult
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.inventory_call.domain.exception.InvalidInventoryCallStateException
import com.dozycoffee.wms.inventory_call.domain.exception.InventoryCallNotFoundException
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.NOW
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

@WithDozyPrincipal(roles = ["wms:warehouse_admin"])
@WebFluxTest(InventoryCallController::class)
class InventoryCallControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getInventoryCallUseCase: GetInventoryCallUseCase

    @MockitoBean
    private lateinit var requeueInventoryCallUseCase: RequeueInventoryCallUseCase

    @MockitoBean
    private lateinit var discardInventoryCallUseCase: DiscardInventoryCallUseCase

    private fun result(status: InventoryCallStatus = InventoryCallStatus.DEAD): InventoryCallResult =
        InventoryCallResult(
            7L, InventoryCallType.INBOUND_RECEIPT, 1L, "wms-inbound-receipt-7", "/api/v1/inbound-receipts",
            "{\"quantity\":10}", status, 1, NOW, "HTTP 404", "{\"code\":\"INV_PRODUCT_NOT_FOUND\"}", null, NOW
        )

    @Nested
    inner class 조회 {

        @Test
        fun `상태로 걸러 목록을 조회하고 본문은 싣지 않는다`() {
            whenever(getInventoryCallUseCase.getAll(InventoryCallStatus.DEAD, 100)).thenReturn(flowOf(result()))

            webTestClient.get().uri("/api/inventory-calls?status=DEAD")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].inventoryCallId").isEqualTo(7)
                .jsonPath("$[0].status").isEqualTo("DEAD")
                .jsonPath("$[0].lastError").isEqualTo("HTTP 404")
                .jsonPath("$[0].requestBody").doesNotExist()
        }

        @Test
        fun `limit는 1에서 500 사이로 보정한다`() {
            whenever(getInventoryCallUseCase.getAll(null, 500)).thenReturn(flowOf())

            webTestClient.get().uri("/api/inventory-calls?limit=9999").exchange().expectStatus().isOk
        }

        @Test
        fun `단건 조회는 요청과 응답 본문을 함께 준다`() {
            runBlocking { whenever(getInventoryCallUseCase.getById(7L)).thenReturn(result()) }

            webTestClient.get().uri("/api/inventory-calls/7")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.requestBody").isEqualTo("{\"quantity\":10}")
                .jsonPath("$.responseBody").isEqualTo("{\"code\":\"INV_PRODUCT_NOT_FOUND\"}")
        }

        @Test
        fun `없는 호출이면 404를 반환한다`() {
            runBlocking { whenever(getInventoryCallUseCase.getById(9L)).thenThrow(InventoryCallNotFoundException()) }

            webTestClient.get().uri("/api/inventory-calls/9")
                .exchange()
                .expectStatus().isNotFound
                .expectBody().jsonPath("$.code").isEqualTo("INVENTORY_CALL_NOT_FOUND")
        }
    }

    @Nested
    inner class 재전송과_버림 {

        @Test
        fun `재전송하면 PENDING으로 돌아간 호출을 반환한다`() {
            runBlocking { whenever(requeueInventoryCallUseCase.requeue(eq(7L))).thenReturn(result(InventoryCallStatus.PENDING)) }

            webTestClient.post().uri("/api/inventory-calls/7/requeue")
                .exchange()
                .expectStatus().isOk
                .expectBody().jsonPath("$.status").isEqualTo("PENDING")
        }

        @Test
        fun `DEAD가 아니면 재전송은 409다`() {
            runBlocking { whenever(requeueInventoryCallUseCase.requeue(eq(7L))).thenThrow(InvalidInventoryCallStateException()) }

            webTestClient.post().uri("/api/inventory-calls/7/requeue")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("INVENTORY_CALL_INVALID_STATE_TRANSITION")
        }

        @Test
        fun `버리면 DISCARDED 호출을 반환한다`() {
            runBlocking { whenever(discardInventoryCallUseCase.discard(eq(7L))).thenReturn(result(InventoryCallStatus.DISCARDED)) }

            webTestClient.post().uri("/api/inventory-calls/7/discard")
                .exchange()
                .expectStatus().isOk
                .expectBody().jsonPath("$.status").isEqualTo("DISCARDED")
        }
    }

    @Test
    @WithDozyPrincipal(roles = ["wms:inbound_manager"])
    fun `warehouse_admin이 아니면 403이다`() {
        webTestClient.get().uri("/api/inventory-calls").exchange().expectStatus().isForbidden
        webTestClient.post().uri("/api/inventory-calls/7/requeue").exchange().expectStatus().isForbidden
    }
}
