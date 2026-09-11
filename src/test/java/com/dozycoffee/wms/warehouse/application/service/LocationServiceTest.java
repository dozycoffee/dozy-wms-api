package com.dozycoffee.wms.warehouse.application.service;

import com.dozycoffee.wms.warehouse.application.port.in.OccupyLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.in.ReleaseLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.out.LocationRepository;
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException;
import com.dozycoffee.wms.warehouse.domain.exception.LocationNotFoundException;
import com.dozycoffee.wms.warehouse.domain.model.Location;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.location;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    @Nested
    class 위치_등록 {

        @Test
        void 정상적인_정보로_등록하면_저장된_위치_정보를_반환한다() {
            RegisterLocationCommand command = new RegisterLocationCommand(1L, "A-01", 70);
            Location saved = location().locationId(1L).build();
            when(locationRepository.save(any(Location.class))).thenReturn(Mono.just(saved));

            StepVerifier.create(locationService.register(command))
                    .assertNext(result -> {
                        assertThat(result.locationId()).isEqualTo(1L);
                        assertThat(result.locationCode()).isEqualTo("A-01");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    class 위치_점유 {

        @Test
        void 여유_용량이_있으면_점유량이_증가한다() {
            Location target = location().locationId(1L).usedCapacity(20).build();
            when(locationRepository.findById(1L)).thenReturn(Mono.just(target));
            when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(locationService.occupy(new OccupyLocationCommand(1L, 10)))
                    .assertNext(result -> assertThat(result.usedCapacity()).isEqualTo(30))
                    .verifyComplete();
        }

        @Test
        void 존재하지_않는_위치를_점유하려_하면_예외를_던진다() {
            when(locationRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(locationService.occupy(new OccupyLocationCommand(1L, 10)))
                    .verifyError(LocationNotFoundException.class);
        }
    }

    @Nested
    class 위치_반출 {

        @Test
        void 사용량보다_많이_반출하면_예외를_던진다() {
            Location target = location().locationId(1L).usedCapacity(5).build();
            when(locationRepository.findById(1L)).thenReturn(Mono.just(target));

            StepVerifier.create(locationService.release(new ReleaseLocationCommand(1L, 10)))
                    .verifyError(InsufficientLocationCapacityException.class);
        }
    }

    @Nested
    class 위치_단건_조회 {

        @Test
        void 존재하는_위치를_조회하면_결과를_반환한다() {
            Location found = location().locationId(1L).build();
            when(locationRepository.findById(1L)).thenReturn(Mono.just(found));

            StepVerifier.create(locationService.getById(1L))
                    .assertNext(result -> assertThat(result.locationId()).isEqualTo(1L))
                    .verifyComplete();
        }
    }
}
