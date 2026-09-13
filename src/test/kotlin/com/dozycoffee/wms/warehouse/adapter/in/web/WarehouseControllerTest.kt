package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterWarehouseRequest
import com.dozycoffee.wms.warehouse.application.port.`in`.ActivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.DeactivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.Mockito.`when`

@WebFluxTest(WarehouseController::class)
class WarehouseControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var registerWarehouseUseCase: RegisterWarehouseUseCase

    @MockitoBean
    private lateinit var activateWarehouseUseCase: ActivateWarehouseUseCase

    @MockitoBean
    private lateinit var deactivateWarehouseUseCase: DeactivateWarehouseUseCase

    @MockitoBean
    private lateinit var getWarehouseUseCase: GetWarehouseUseCase

    private fun sampleResult(): WarehouseResult {
        return WarehouseResult(
            1L, "도지하우스 제주 센터", "제주특별자치도 제주시",
            BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312),
            AvailabilityStatus.AVAILABLE
        )
    }

    @Nested
    inner class 창고_등록 {

        @Test
        fun `유효한 요청이면 201과 등록된 창고를 반환한다`() {
            `when`(registerWarehouseUseCase.register(any())).thenReturn(Mono.just(sampleResult()))

            webTestClient.post().uri("/api/warehouses")
                .bodyValue(
                    RegisterWarehouseRequest(
                        "도지하우스 제주 센터", "제주특별자치도 제주시",
                        BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312)
                    )
                )
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.warehouseId").isEqualTo(1)
                .jsonPath("$.warehouseStatus").isEqualTo("AVAILABLE")
        }

        @Test
        fun `필수값이 비어있으면 400을 반환한다`() {
            webTestClient.post().uri("/api/warehouses")
                .bodyValue(RegisterWarehouseRequest("", "", null, null))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class 창고_단건_조회 {

        @Test
        fun `존재하면 200과 창고 정보를 반환한다`() {
            `when`(getWarehouseUseCase.getById(1L)).thenReturn(Mono.just(sampleResult()))

            webTestClient.get().uri("/api/warehouses/{warehouseId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.warehouseId").isEqualTo(1)
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            `when`(getWarehouseUseCase.getById(eq(999L))).thenReturn(Mono.error(WarehouseNotFoundException()))

            webTestClient.get().uri("/api/warehouses/{warehouseId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 창고_활성화_비활성화 {

        @Test
        fun `활성화 요청 시 200과 활성화된 창고를 반환한다`() {
            `when`(activateWarehouseUseCase.activate(1L)).thenReturn(Mono.just(sampleResult()))

            webTestClient.patch().uri("/api/warehouses/{warehouseId}/activate", 1L)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        fun `비활성화 요청 시 200과 비활성화된 창고를 반환한다`() {
            val deactivated = WarehouseResult(
                1L, "도지하우스 제주 센터", "제주특별자치도 제주시",
                BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312),
                AvailabilityStatus.UNAVAILABLE
            )
            `when`(deactivateWarehouseUseCase.deactivate(1L)).thenReturn(Mono.just(deactivated))

            webTestClient.patch().uri("/api/warehouses/{warehouseId}/deactivate", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.warehouseStatus").isEqualTo("UNAVAILABLE")
        }
    }
}
