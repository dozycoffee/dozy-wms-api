package com.dozycoffee.wms.warehouse.application.service;


import com.dozycoffee.wms.warehouse.application.port.in.command.OccupyWorkAreaCommand;
import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterWorkAreaCommand;
import com.dozycoffee.wms.warehouse.application.port.in.command.ReleaseWorkAreaCommand;
import com.dozycoffee.wms.warehouse.application.port.out.WorkAreaRepository;
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaCapacityExceededException;
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaNotFoundException;
import com.dozycoffee.wms.warehouse.domain.model.WorkArea;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static com.dozycoffee.wms.warehouse.fixture.WorkAreaTestBuilder.workArea;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkAreaServiceTest {

    @Mock
    private WorkAreaRepository workAreaRepository;

    @InjectMocks
    private WorkAreaService workAreaService;

    @Nested
    class 작업구역_등록 {

        @Test
        void 정상적인_정보로_등록하면_저장된_작업구역_정보를_반환한다() {
            RegisterWorkAreaCommand command = new RegisterWorkAreaCommand(1L, AreaCode.INBOUND);
            WorkArea saved = workArea().workAreaId(1L).build();
            when(workAreaRepository.save(any(WorkArea.class))).thenReturn(Mono.just(saved));

            StepVerifier.create(workAreaService.register(command))
                    .assertNext(result -> {
                        assertThat(result.workAreaId()).isEqualTo(1L);
                        assertThat(result.areaCode()).isEqualTo(AreaCode.INBOUND);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    class 작업구역_점유 {

        @Test
        void 여유_용량이_있으면_점유량이_증가한다() {
            WorkArea target = workArea().workAreaId(1L).usedCapacity(10).build();
            when(workAreaRepository.findById(1L)).thenReturn(Mono.just(target));
            when(workAreaRepository.save(any(WorkArea.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(workAreaService.occupy(new OccupyWorkAreaCommand(1L, 5)))
                    .assertNext(result -> assertThat(result.usedCapacity()).isEqualTo(15))
                    .verifyComplete();
        }

        @Test
        void 최대_용량을_초과하면_예외를_던진다() {
            WorkArea target = workArea().workAreaId(1L).usedCapacity(AreaCode.INBOUND.getCapacity().value()).build();
            when(workAreaRepository.findById(1L)).thenReturn(Mono.just(target));

            StepVerifier.create(workAreaService.occupy(new OccupyWorkAreaCommand(1L, 1)))
                    .verifyError(WorkAreaCapacityExceededException.class);
        }

        @Test
        void 존재하지_않는_작업구역을_점유하려_하면_예외를_던진다() {
            when(workAreaRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(workAreaService.occupy(new OccupyWorkAreaCommand(1L, 5)))
                    .verifyError(WorkAreaNotFoundException.class);
        }
    }

    @Nested
    class 작업구역_반출 {

        @Test
        void 사용량_범위_내에서_반출하면_점유량이_감소한다() {
            WorkArea target = workArea().workAreaId(1L).usedCapacity(10).build();
            when(workAreaRepository.findById(1L)).thenReturn(Mono.just(target));
            when(workAreaRepository.save(any(WorkArea.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(workAreaService.release(new ReleaseWorkAreaCommand(1L, 4)))
                    .assertNext(result -> assertThat(result.usedCapacity()).isEqualTo(6))
                    .verifyComplete();
        }
    }

    @Nested
    class 작업구역_단건_조회 {

        @Test
        void 존재하지_않는_작업구역을_조회하면_예외를_던진다() {
            when(workAreaRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(workAreaService.getById(1L))
                    .verifyError(WorkAreaNotFoundException.class);
        }
    }
}
