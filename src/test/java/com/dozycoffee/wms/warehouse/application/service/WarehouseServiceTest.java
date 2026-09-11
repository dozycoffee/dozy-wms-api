package com.dozycoffee.wms.warehouse.application.service;

import com.dozycoffee.wms.warehouse.application.port.in.RegisterWarehouseCommand;
import com.dozycoffee.wms.warehouse.application.port.in.WarehouseResult;
import com.dozycoffee.wms.warehouse.application.port.out.WarehouseRepository;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException;
import com.dozycoffee.wms.warehouse.domain.model.Warehouse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    @Nested
    class 창고_등록 {

        @Test
        void 정상적인_정보로_등록하면_저장된_창고_정보를_반환한다() {
            RegisterWarehouseCommand command = new RegisterWarehouseCommand(
                    "도지하우스 제주 센터", "제주특별자치도 제주시", BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312)
            );
            Warehouse saved = warehouse().warehouseId(1L).build();
            when(warehouseRepository.save(any(Warehouse.class))).thenReturn(Mono.just(saved));

            StepVerifier.create(warehouseService.register(command))
                    .assertNext(result -> {
                        assertThat(result.warehouseId()).isEqualTo(1L);
                        assertThat(result.warehouseStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
                    })
                    .verifyComplete();

            ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
            verify(warehouseRepository).save(captor.capture());
            assertThat(captor.getValue().getWarehouseStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        }
    }

    @Nested
    class 창고_활성화 {

        @Test
        void 존재하는_창고를_활성화하면_상태가_AVAILABLE로_바뀐다() {
            Warehouse inactive = warehouse().warehouseId(1L).warehouseStatus(AvailabilityStatus.UNAVAILABLE).build();
            when(warehouseRepository.findById(1L)).thenReturn(Mono.just(inactive));
            when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(warehouseService.activate(1L))
                    .assertNext(result -> assertThat(result.warehouseStatus()).isEqualTo(AvailabilityStatus.AVAILABLE))
                    .verifyComplete();
        }

        @Test
        void 존재하지_않는_창고를_활성화하면_예외를_던진다() {
            when(warehouseRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(warehouseService.activate(1L))
                    .verifyError(WarehouseNotFoundException.class);
        }
    }

    @Nested
    class 창고_비활성화 {

        @Test
        void 존재하는_창고를_비활성화하면_상태가_UNAVAILABLE로_바뀐다() {
            Warehouse active = warehouse().warehouseId(1L).warehouseStatus(AvailabilityStatus.AVAILABLE).build();
            when(warehouseRepository.findById(1L)).thenReturn(Mono.just(active));
            when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(warehouseService.deactivate(1L))
                    .assertNext(result -> assertThat(result.warehouseStatus()).isEqualTo(AvailabilityStatus.UNAVAILABLE))
                    .verifyComplete();
        }

        @Test
        void 존재하지_않는_창고를_비활성화하면_예외를_던진다() {
            when(warehouseRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(warehouseService.deactivate(1L))
                    .verifyError(WarehouseNotFoundException.class);
        }
    }

    @Nested
    class 창고_단건_조회 {

        @Test
        void 존재하는_창고를_조회하면_결과를_반환한다() {
            Warehouse found = warehouse().warehouseId(1L).build();
            when(warehouseRepository.findById(1L)).thenReturn(Mono.just(found));

            StepVerifier.create(warehouseService.getById(1L))
                    .assertNext(result -> assertThat(result.warehouseId()).isEqualTo(1L))
                    .verifyComplete();
        }

        @Test
        void 존재하지_않는_창고를_조회하면_예외를_던진다() {
            when(warehouseRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(warehouseService.getById(1L))
                    .verifyError(WarehouseNotFoundException.class);
        }
    }
}
