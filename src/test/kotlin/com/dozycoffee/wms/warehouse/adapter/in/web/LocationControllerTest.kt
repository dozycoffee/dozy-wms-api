package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.AmountRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterLocationRequest
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException
import com.dozycoffee.wms.warehouse.domain.exception.LocationNotFoundException
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

@WebFluxTest(LocationController::class)
class LocationControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerLocationUseCase: RegisterLocationUseCase

    @MockitoBean
    private lateinit var occupyLocationUseCase: OccupyLocationUseCase

    @MockitoBean
    private lateinit var releaseLocationUseCase: ReleaseLocationUseCase

    @MockitoBean
    private lateinit var getLocationUseCase: GetLocationUseCase

    private fun sampleResult(usedCapacity: Int): LocationResult {
        return LocationResult(1L, 1L, "A-01", 70, usedCapacity, AvailabilityStatus.AVAILABLE)
    }

    @Nested
    inner class 위치_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 위치를 반환한다`() {
            `when`(registerLocationUseCase.register(any())).thenReturn(Mono.just(sampleResult(0)))

            webTestClient.post().uri("/api/zones/{zoneId}/locations", 1L)
                .bodyValue(RegisterLocationRequest("A-01", 70))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.locationCode").isEqualTo("A-01")
        }

        @Test
        fun `최대 용량이 0 이하이면 400을 반환한다`() {
            webTestClient.post().uri("/api/zones/{zoneId}/locations", 1L)
                .bodyValue(RegisterLocationRequest("A-01", 0))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 위치_단건_조회 {

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            `when`(getLocationUseCase.getById(eq(999L))).thenReturn(Mono.error(LocationNotFoundException()))

            webTestClient.get().uri("/api/locations/{locationId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 위치_점유_반출 {

        @Test
        fun `점유 요청이 유효하면 200을 반환한다`() {
            `when`(occupyLocationUseCase.occupy(any())).thenReturn(Mono.just(sampleResult(30)))

            webTestClient.patch().uri("/api/locations/{locationId}/occupy", 1L)
                .bodyValue(AmountRequest(30))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.usedCapacity").isEqualTo(30)
        }

        @Test
        fun `사용량보다 많은 반출 요청이면 409를 반환한다`() {
            `when`(releaseLocationUseCase.release(any())).thenReturn(Mono.error(InsufficientLocationCapacityException()))

            webTestClient.patch().uri("/api/locations/{locationId}/release", 1L)
                .bodyValue(AmountRequest(100))
                .exchange()
                .expectStatus().isEqualTo(409)
        }
    }
}
