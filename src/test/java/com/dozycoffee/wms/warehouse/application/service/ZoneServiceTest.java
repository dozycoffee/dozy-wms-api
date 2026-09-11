package com.dozycoffee.wms.warehouse.application.service;

import com.dozycoffee.wms.warehouse.application.port.in.RegisterZoneCommand;
import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.exception.ZoneNotFoundException;
import com.dozycoffee.wms.warehouse.domain.model.Zone;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.zone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private ZoneService zoneService;

    @Nested
    class 구역_등록 {

        @Test
        void 정상적인_정보로_등록하면_저장된_구역_정보를_반환한다() {
            RegisterZoneCommand command = new RegisterZoneCommand(1L, ZoneCode.A);
            Zone saved = zone().zoneId(1L).build();
            when(zoneRepository.save(any(Zone.class))).thenReturn(Mono.just(saved));

            StepVerifier.create(zoneService.register(command))
                    .assertNext(result -> {
                        assertThat(result.zoneId()).isEqualTo(1L);
                        assertThat(result.zoneCode()).isEqualTo(ZoneCode.A);
                        assertThat(result.zoneStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    class 구역_단건_조회 {

        @Test
        void 존재하는_구역을_조회하면_결과를_반환한다() {
            Zone found = zone().zoneId(1L).build();
            when(zoneRepository.findById(1L)).thenReturn(Mono.just(found));

            StepVerifier.create(zoneService.getById(1L))
                    .assertNext(result -> assertThat(result.zoneId()).isEqualTo(1L))
                    .verifyComplete();
        }

        @Test
        void 존재하지_않는_구역을_조회하면_예외를_던진다() {
            when(zoneRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(zoneService.getById(1L))
                    .verifyError(ZoneNotFoundException.class);
        }
    }
}
