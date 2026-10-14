package com.dozycoffee.wms.warehouse_member.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.warehouse_member.application.port.`in`.RepublishWarehouseAccessUseCase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient

@WithDozyPrincipal(roles = ["wms:admin"])
@WebFluxTest(WarehouseAccessEventController::class)
class WarehouseAccessEventControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var republishWarehouseAccessUseCase: RepublishWarehouseAccessUseCase

    @Test
    fun `재발행하면 만든 직원 수를 반환한다`() {
        runBlocking { whenever(republishWarehouseAccessUseCase.republishAll()).thenReturn(7) }

        webTestClient.post().uri("/api/warehouse-members/republish")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.principalCount").isEqualTo(7)
    }

    @Test
    @WithDozyPrincipal(roles = ["wms:warehouse_manager"])
    fun `admin이 아니면 403이다`() {
        webTestClient.post().uri("/api/warehouse-members/republish").exchange().expectStatus().isForbidden
    }

    @Test
    @WithDozyPrincipal(roles = ["wms:service"])
    fun `서비스 role도 403이다`() {
        webTestClient.post().uri("/api/warehouse-members/republish").exchange().expectStatus().isForbidden
    }
}
