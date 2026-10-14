package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterWarehouseRequest
import com.dozycoffee.wms.warehouse.application.port.`in`.ActivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.DeactivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ListWarehousesUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@WithDozyPrincipal(roles = ["wms:admin"])
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

    @MockitoBean
    private lateinit var listWarehousesUseCase: ListWarehousesUseCase

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
            runBlocking { whenever(registerWarehouseUseCase.register(any())).thenReturn(sampleResult()) }

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
            runBlocking { whenever(getWarehouseUseCase.getById(1L)).thenReturn(sampleResult()) }

            webTestClient.get().uri("/api/warehouses/{warehouseId}", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.warehouseId").isEqualTo(1)
        }

        @Test
        fun `존재하지 않으면 404를 반환한다`() {
            runBlocking { whenever(getWarehouseUseCase.getById(eq(999L))).thenThrow(WarehouseNotFoundException()) }

            webTestClient.get().uri("/api/warehouses/{warehouseId}", 999L)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class 창고_목록_조회 {

        @Test
        fun `200과 items 래퍼로 창고 목록을 반환한다`() {
            val second = WarehouseResult(
                2L, "경기 물류센터", "경기도 이천시", BigDecimal.valueOf(37.2792), BigDecimal.valueOf(127.4425),
                AvailabilityStatus.UNAVAILABLE
            )
            whenever(listWarehousesUseCase.getAll()).thenReturn(flowOf(sampleResult(), second))

            webTestClient.get().uri("/api/warehouses")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(2)
                .jsonPath("$.items[0].warehouseId").isEqualTo(1)
                .jsonPath("$.items[0].warehouseName").isEqualTo("도지하우스 제주 센터")
                .jsonPath("$.items[0].address").isEqualTo("제주특별자치도 제주시")
                .jsonPath("$.items[0].latitude").isEqualTo(33.4996)
                .jsonPath("$.items[0].longitude").isEqualTo(126.5312)
                .jsonPath("$.items[0].warehouseStatus").isEqualTo("AVAILABLE")
                .jsonPath("$.items[1].warehouseStatus").isEqualTo("UNAVAILABLE")
        }

        @Test
        fun `창고가 없으면 빈 items를 반환한다`() {
            whenever(listWarehousesUseCase.getAll()).thenReturn(emptyFlow())

            webTestClient.get().uri("/api/warehouses")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(0)
        }

        @Test
        @WithDozyPrincipal(roles = ["wms:viewer"])
        fun `viewer이면 목록을 조회할 수 있다`() {
            whenever(listWarehousesUseCase.getAll()).thenReturn(emptyFlow())

            webTestClient.get().uri("/api/warehouses")
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @WithDozyPrincipal(roles = ["wms:warehouse_manager"])
        fun `창고 관리자도 목록을 조회할 수 있다`() {
            whenever(listWarehousesUseCase.getAll()).thenReturn(emptyFlow())

            webTestClient.get().uri("/api/warehouses")
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @WithDozyPrincipal(roles = ["wms:viewer"])
        fun `viewer는 창고를 등록할 수 없다`() {
            webTestClient.post().uri("/api/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapOf("warehouseName" to "권한 없음", "address" to "제주", "latitude" to 33.4, "longitude" to 126.5))
                .exchange()
                .expectStatus().isForbidden
        }

        @Test
        @WithDozyPrincipal(roles = ["wms:service"])
        fun `service role만으로는 403을 반환한다`() {
            webTestClient.get().uri("/api/warehouses")
                .exchange()
                .expectStatus().isForbidden
        }
    }

    @Nested
    inner class 창고_활성화_비활성화 {

        @Test
        fun `활성화 요청 시 200과 활성화된 창고를 반환한다`() {
            runBlocking { whenever(activateWarehouseUseCase.activate(1L)).thenReturn(sampleResult()) }

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
            runBlocking { whenever(deactivateWarehouseUseCase.deactivate(1L)).thenReturn(deactivated) }

            webTestClient.patch().uri("/api/warehouses/{warehouseId}/deactivate", 1L)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.warehouseStatus").isEqualTo("UNAVAILABLE")
        }
    }
}
