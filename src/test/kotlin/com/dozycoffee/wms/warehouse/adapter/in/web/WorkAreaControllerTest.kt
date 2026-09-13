package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.AmountRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterWorkAreaRequest
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaCapacityExceededException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import org.mockito.kotlin.any
import org.mockito.Mockito.`when`

@WebFluxTest(WorkAreaController::class)
class WorkAreaControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerWorkAreaUseCase: RegisterWorkAreaUseCase

    @MockitoBean
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @MockitoBean
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @MockitoBean
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    private fun sampleResult(usedCapacity: Int): WorkAreaResult {
        return WorkAreaResult(1L, 1L, AreaCode.INBOUND, "입고 처리장", 50, usedCapacity, AvailabilityStatus.AVAILABLE)
    }

    @Nested
    inner class 작업구역_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 작업구역을 반환한다`() {
            `when`(registerWorkAreaUseCase.register(any())).thenReturn(Mono.just(sampleResult(0)))

            webTestClient.post().uri("/api/warehouses/{warehouseId}/work-areas", 1L)
                .bodyValue(RegisterWorkAreaRequest(AreaCode.INBOUND))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.workAreaId").isEqualTo(1)
        }
    }

    @Nested
    inner class 작업구역_단건_조회 {

        @Test
        fun `존재하면 200을 반환한다`() {
            `when`(getWorkAreaUseCase.getById(1L)).thenReturn(Mono.just(sampleResult(0)))

            webTestClient.get().uri("/api/work-areas/{workAreaId}", 1L)
                .exchange()
                .expectStatus().isOk
        }
    }

    @Nested
    inner class 작업구역_점유_반출 {

        @Test
        fun `점유 요청이 유효하면 200과 증가한 점유량을 반환한다`() {
            `when`(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.just(sampleResult(10)))

            webTestClient.patch().uri("/api/work-areas/{workAreaId}/occupy", 1L)
                .bodyValue(AmountRequest(10))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.usedCapacity").isEqualTo(10)
        }

        @Test
        fun `점유 수량이 0 이하이면 400을 반환한다`() {
            webTestClient.patch().uri("/api/work-areas/{workAreaId}/occupy", 1L)
                .bodyValue(AmountRequest(0))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `최대 용량을 초과하는 점유 요청이면 409를 반환한다`() {
            `when`(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.error(WorkAreaCapacityExceededException()))

            webTestClient.patch().uri("/api/work-areas/{workAreaId}/occupy", 1L)
                .bodyValue(AmountRequest(100))
                .exchange()
                .expectStatus().isEqualTo(409)
        }

        @Test
        fun `반출 요청이 유효하면 200과 감소한 점유량을 반환한다`() {
            `when`(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(sampleResult(5)))

            webTestClient.patch().uri("/api/work-areas/{workAreaId}/release", 1L)
                .bodyValue(AmountRequest(5))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.usedCapacity").isEqualTo(5)
        }
    }
}
