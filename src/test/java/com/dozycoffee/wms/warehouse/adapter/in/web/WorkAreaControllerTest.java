package com.dozycoffee.wms.warehouse.adapter.in.web;

import com.dozycoffee.wms.warehouse.adapter.in.web.request.AmountRequest;
import com.dozycoffee.wms.warehouse.adapter.in.web.request.RegisterWorkAreaRequest;
import com.dozycoffee.wms.warehouse.application.port.in.GetWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.OccupyWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.ReleaseWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.result.WorkAreaResult;
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaCapacityExceededException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(WorkAreaController.class)
class WorkAreaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RegisterWorkAreaUseCase registerWorkAreaUseCase;

    @MockitoBean
    private OccupyWorkAreaUseCase occupyWorkAreaUseCase;

    @MockitoBean
    private ReleaseWorkAreaUseCase releaseWorkAreaUseCase;

    @MockitoBean
    private GetWorkAreaUseCase getWorkAreaUseCase;

    private static WorkAreaResult sampleResult(int usedCapacity) {
        return new WorkAreaResult(1L, 1L, AreaCode.INBOUND, "입고 처리장", 50, usedCapacity, AvailabilityStatus.AVAILABLE);
    }

    @Nested
    class 작업구역_등록 {

        @Test
        void 유효한_요청이면_201과_등록된_작업구역을_반환한다() {
            when(registerWorkAreaUseCase.register(any())).thenReturn(Mono.just(sampleResult(0)));

            webTestClient.post().uri("/api/warehouses/{warehouseId}/work-areas", 1L)
                    .bodyValue(new RegisterWorkAreaRequest(AreaCode.INBOUND))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.workAreaId").isEqualTo(1);
        }
    }

    @Nested
    class 작업구역_단건_조회 {

        @Test
        void 존재하면_200을_반환한다() {
            when(getWorkAreaUseCase.getById(1L)).thenReturn(Mono.just(sampleResult(0)));

            webTestClient.get().uri("/api/work-areas/{workAreaId}", 1L)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    class 작업구역_점유_반출 {

        @Test
        void 점유_요청이_유효하면_200과_증가한_점유량을_반환한다() {
            when(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.just(sampleResult(10)));

            webTestClient.patch().uri("/api/work-areas/{workAreaId}/occupy", 1L)
                    .bodyValue(new AmountRequest(10))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.usedCapacity").isEqualTo(10);
        }

        @Test
        void 점유_수량이_0_이하이면_400을_반환한다() {
            webTestClient.patch().uri("/api/work-areas/{workAreaId}/occupy", 1L)
                    .bodyValue(new AmountRequest(0))
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        void 최대_용량을_초과하는_점유_요청이면_409를_반환한다() {
            when(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.error(new WorkAreaCapacityExceededException()));

            webTestClient.patch().uri("/api/work-areas/{workAreaId}/occupy", 1L)
                    .bodyValue(new AmountRequest(100))
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }

        @Test
        void 반출_요청이_유효하면_200과_감소한_점유량을_반환한다() {
            when(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(sampleResult(5)));

            webTestClient.patch().uri("/api/work-areas/{workAreaId}/release", 1L)
                    .bodyValue(new AmountRequest(5))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.usedCapacity").isEqualTo(5);
        }
    }
}
