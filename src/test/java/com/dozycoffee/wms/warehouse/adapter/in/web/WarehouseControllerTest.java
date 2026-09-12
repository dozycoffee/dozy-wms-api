package com.dozycoffee.wms.warehouse.adapter.in.web;

import com.dozycoffee.wms.warehouse.adapter.in.web.request.RegisterWarehouseRequest;
import com.dozycoffee.wms.warehouse.application.port.in.ActivateWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.DeactivateWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.GetWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.result.WarehouseResult;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(WarehouseController.class)
class WarehouseControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RegisterWarehouseUseCase registerWarehouseUseCase;

    @MockitoBean
    private ActivateWarehouseUseCase activateWarehouseUseCase;

    @MockitoBean
    private DeactivateWarehouseUseCase deactivateWarehouseUseCase;

    @MockitoBean
    private GetWarehouseUseCase getWarehouseUseCase;

    private static WarehouseResult sampleResult() {
        return new WarehouseResult(
                1L, "도지하우스 제주 센터", "제주특별자치도 제주시",
                BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312),
                AvailabilityStatus.AVAILABLE
        );
    }

    @Nested
    class 창고_등록 {

        @Test
        void 유효한_요청이면_201과_등록된_창고를_반환한다() {
            when(registerWarehouseUseCase.register(any())).thenReturn(Mono.just(sampleResult()));

            webTestClient.post().uri("/api/warehouses")
                    .bodyValue(new RegisterWarehouseRequest(
                            "도지하우스 제주 센터", "제주특별자치도 제주시",
                            BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312)
                    ))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.warehouseId").isEqualTo(1)
                    .jsonPath("$.warehouseStatus").isEqualTo("AVAILABLE");
        }

        @Test
        void 필수값이_비어있으면_400을_반환한다() {
            webTestClient.post().uri("/api/warehouses")
                    .bodyValue(new RegisterWarehouseRequest("", "", null, null))
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    class 창고_단건_조회 {

        @Test
        void 존재하면_200과_창고_정보를_반환한다() {
            when(getWarehouseUseCase.getById(1L)).thenReturn(Mono.just(sampleResult()));

            webTestClient.get().uri("/api/warehouses/{warehouseId}", 1L)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.warehouseId").isEqualTo(1);
        }

        @Test
        void 존재하지_않으면_404를_반환한다() {
            when(getWarehouseUseCase.getById(eq(999L))).thenReturn(Mono.error(new WarehouseNotFoundException()));

            webTestClient.get().uri("/api/warehouses/{warehouseId}", 999L)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    class 창고_활성화_비활성화 {

        @Test
        void 활성화_요청_시_200과_활성화된_창고를_반환한다() {
            when(activateWarehouseUseCase.activate(1L)).thenReturn(Mono.just(sampleResult()));

            webTestClient.patch().uri("/api/warehouses/{warehouseId}/activate", 1L)
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        void 비활성화_요청_시_200과_비활성화된_창고를_반환한다() {
            WarehouseResult deactivated = new WarehouseResult(
                    1L, "도지하우스 제주 센터", "제주특별자치도 제주시",
                    BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312),
                    AvailabilityStatus.UNAVAILABLE
            );
            when(deactivateWarehouseUseCase.deactivate(1L)).thenReturn(Mono.just(deactivated));

            webTestClient.patch().uri("/api/warehouses/{warehouseId}/deactivate", 1L)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.warehouseStatus").isEqualTo("UNAVAILABLE");
        }
    }
}
