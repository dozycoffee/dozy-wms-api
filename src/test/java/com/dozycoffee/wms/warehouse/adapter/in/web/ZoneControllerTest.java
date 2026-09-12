package com.dozycoffee.wms.warehouse.adapter.in.web;

import com.dozycoffee.wms.warehouse.adapter.in.web.request.RegisterZoneRequest;
import com.dozycoffee.wms.warehouse.application.port.in.GetZoneUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterZoneUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.result.ZoneResult;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.exception.ZoneNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(ZoneController.class)
class ZoneControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RegisterZoneUseCase registerZoneUseCase;

    @MockitoBean
    private GetZoneUseCase getZoneUseCase;

    private static ZoneResult sampleResult() {
        return new ZoneResult(1L, 1L, ZoneCode.A, "원두", TemperatureType.AMBIENT, 180, AvailabilityStatus.AVAILABLE);
    }

    @Nested
    class 구역_등록 {

        @Test
        void 유효한_요청이면_201과_등록된_구역을_반환한다() {
            when(registerZoneUseCase.register(any())).thenReturn(Mono.just(sampleResult()));

            webTestClient.post().uri("/api/warehouses/{warehouseId}/zones", 1L)
                    .bodyValue(new RegisterZoneRequest(ZoneCode.A))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.zoneId").isEqualTo(1)
                    .jsonPath("$.zoneCode").isEqualTo("A");
        }

        @Test
        void 구역_코드가_없으면_400을_반환한다() {
            webTestClient.post().uri("/api/warehouses/{warehouseId}/zones", 1L)
                    .bodyValue(new RegisterZoneRequest(null))
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    class 구역_단건_조회 {

        @Test
        void 존재하면_200과_구역_정보를_반환한다() {
            when(getZoneUseCase.getById(1L)).thenReturn(Mono.just(sampleResult()));

            webTestClient.get().uri("/api/zones/{zoneId}", 1L)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.zoneId").isEqualTo(1);
        }

        @Test
        void 존재하지_않으면_404를_반환한다() {
            when(getZoneUseCase.getById(eq(999L))).thenReturn(Mono.error(new ZoneNotFoundException()));

            webTestClient.get().uri("/api/zones/{zoneId}", 999L)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }
}
