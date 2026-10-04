package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@WithDozyPrincipal(roles = ["wms:warehouse_admin"])
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
            runBlocking { whenever(registerLocationUseCase.register(any())).thenReturn(sampleResult(0)) }

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
            runBlocking { whenever(getLocationUseCase.getById(eq(999L))).thenThrow(LocationNotFoundException()) }

            webTestClient.get().uri("/api/locations/{locationId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class Zone_기준_목록_조회 {

        @Test
        fun `Zone에 속한 위치 목록을 반환한다`() {
            runBlocking {
                whenever(getLocationUseCase.getByZoneId(eq(1L)))
                    .thenReturn(flowOf(sampleResult(0), sampleResult(10)))
            }

            webTestClient.get().uri("/api/zones/{zoneId}/locations", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
        }
    }

    @Nested
    inner class 위치_점유_반출 {

        @Test
        fun `점유 요청이 유효하면 200을 반환한다`() {
            runBlocking { whenever(occupyLocationUseCase.occupy(any())).thenReturn(sampleResult(30)) }

            webTestClient.patch().uri("/api/locations/{locationId}/occupy", 1L)
                .bodyValue(AmountRequest(30))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.usedCapacity").isEqualTo(30)
        }

        @Test
        fun `사용량보다 많은 반출 요청이면 409를 반환한다`() {
            runBlocking { whenever(releaseLocationUseCase.release(any())).thenThrow(InsufficientLocationCapacityException()) }

            webTestClient.patch().uri("/api/locations/{locationId}/release", 1L)
                .bodyValue(AmountRequest(100))
                .exchange()
                .expectStatus().isEqualTo(409)
        }
    }
}
