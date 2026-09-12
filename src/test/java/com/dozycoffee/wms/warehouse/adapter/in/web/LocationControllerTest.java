package com.dozycoffee.wms.warehouse.adapter.in.web;

import com.dozycoffee.wms.warehouse.adapter.in.web.request.AmountRequest;
import com.dozycoffee.wms.warehouse.adapter.in.web.request.RegisterLocationRequest;
import com.dozycoffee.wms.warehouse.application.port.in.GetLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.OccupyLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.ReleaseLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.result.LocationResult;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException;
import com.dozycoffee.wms.warehouse.domain.exception.LocationNotFoundException;
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

@WebFluxTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RegisterLocationUseCase registerLocationUseCase;

    @MockitoBean
    private OccupyLocationUseCase occupyLocationUseCase;

    @MockitoBean
    private ReleaseLocationUseCase releaseLocationUseCase;

    @MockitoBean
    private GetLocationUseCase getLocationUseCase;

    private static LocationResult sampleResult(int usedCapacity) {
        return new LocationResult(1L, 1L, "A-01", 70, usedCapacity, AvailabilityStatus.AVAILABLE);
    }

    @Nested
    class 위치_등록 {

        @Test
        void 유효한_요청이면_201과_등록된_위치를_반환한다() {
            when(registerLocationUseCase.register(any())).thenReturn(Mono.just(sampleResult(0)));

            webTestClient.post().uri("/api/zones/{zoneId}/locations", 1L)
                    .bodyValue(new RegisterLocationRequest("A-01", 70))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.locationCode").isEqualTo("A-01");
        }

        @Test
        void 최대_용량이_0_이하이면_400을_반환한다() {
            webTestClient.post().uri("/api/zones/{zoneId}/locations", 1L)
                    .bodyValue(new RegisterLocationRequest("A-01", 0))
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    class 위치_단건_조회 {

        @Test
        void 존재하지_않으면_404를_반환한다() {
            when(getLocationUseCase.getById(eq(999L))).thenReturn(Mono.error(new LocationNotFoundException()));

            webTestClient.get().uri("/api/locations/{locationId}", 999L)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    class 위치_점유_반출 {

        @Test
        void 점유_요청이_유효하면_200을_반환한다() {
            when(occupyLocationUseCase.occupy(any())).thenReturn(Mono.just(sampleResult(30)));

            webTestClient.patch().uri("/api/locations/{locationId}/occupy", 1L)
                    .bodyValue(new AmountRequest(30))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.usedCapacity").isEqualTo(30);
        }

        @Test
        void 사용량보다_많은_반출_요청이면_409를_반환한다() {
            when(releaseLocationUseCase.release(any())).thenReturn(Mono.error(new InsufficientLocationCapacityException()));

            webTestClient.patch().uri("/api/locations/{locationId}/release", 1L)
                    .bodyValue(new AmountRequest(100))
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }
    }
}
