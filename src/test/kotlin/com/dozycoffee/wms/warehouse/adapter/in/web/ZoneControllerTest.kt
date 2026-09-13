package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterZoneRequest
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.exception.ZoneNotFoundException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.Mockito.`when`

@WebFluxTest(ZoneController::class)
class ZoneControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerZoneUseCase: RegisterZoneUseCase

    @MockitoBean
    private lateinit var getZoneUseCase: GetZoneUseCase

    private fun sampleResult(): ZoneResult {
        return ZoneResult(1L, 1L, ZoneCode.A, "원두", TemperatureType.AMBIENT, 180, AvailabilityStatus.AVAILABLE)
    }

    @Nested
    inner class 구역_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 구역을 반환한다`() {
            `when`(registerZoneUseCase.register(any())).thenReturn(Mono.just(sampleResult()))

            webTestClient.post().uri("/api/warehouses/{warehouseId}/zones", 1L)
                .bodyValue(RegisterZoneRequest(ZoneCode.A))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.zoneId").isEqualTo(1)
                .jsonPath("$.zoneCode").isEqualTo("A")
        }

        @Test
        fun `구역 코드가 없으면 400을 반환한다`() {
            webTestClient.post().uri("/api/warehouses/{warehouseId}/zones", 1L)
                .bodyValue(RegisterZoneRequest(null))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 구역_단건_조회 {

        @Test
        fun `존재하면 200과 구역 정보를 반환한다`() {
            `when`(getZoneUseCase.getById(1L)).thenReturn(Mono.just(sampleResult()))

            webTestClient.get().uri("/api/zones/{zoneId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.zoneId").isEqualTo(1)
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            `when`(getZoneUseCase.getById(eq(999L))).thenReturn(Mono.error(ZoneNotFoundException()))

            webTestClient.get().uri("/api/zones/{zoneId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }
}
