package com.dozycoffee.wms.outbound.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundAllocationUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundAllocationResult
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate

@WithDozyPrincipal(roles = ["wms:outbound_manager"])
@WebFluxTest(OutboundAllocationController::class)
class OutboundAllocationControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var getOutboundAllocationUseCase: GetOutboundAllocationUseCase

    @Test
    fun `출고의 Lot 할당 목록을 반환한다`() {
        runBlocking {
            whenever(getOutboundAllocationUseCase.getAllocationsByOutbound(1L)).thenReturn(
                listOf(OutboundAllocationResult(5L, 1L, 100L, 55L, "A", LocalDate.of(2027, 1, 1), 4, null))
            )
        }

        webTestClient.get().uri("/api/outbound-allocations?outboundId=1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].outboundAllocationId").isEqualTo(5)
            .jsonPath("$[0].productId").isEqualTo(100)
            .jsonPath("$[0].inventoryId").isEqualTo(55)
            .jsonPath("$[0].expirationDate").isEqualTo("2027-01-01")
            .jsonPath("$[0].quantity").isEqualTo(4)
    }

    @Test
    fun `존재하지 않는 출고이면 404를 반환한다`() {
        runBlocking { whenever(getOutboundAllocationUseCase.getAllocationsByOutbound(999L)).thenThrow(OutboundNotFoundException()) }

        webTestClient.get().uri("/api/outbound-allocations?outboundId=999")
            .exchange()
            .expectStatus().isNotFound
    }
}
