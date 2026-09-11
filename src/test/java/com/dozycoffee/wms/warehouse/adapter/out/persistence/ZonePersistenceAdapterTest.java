package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.global.config.R2dbcConfig;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.model.Zone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.warehouse;
import static com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.zone;
import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({R2dbcConfig.class, WarehousePersistenceAdapter.class, ZonePersistenceAdapter.class})
class ZonePersistenceAdapterTest {

    @Autowired
    private WarehousePersistenceAdapter warehousePersistenceAdapter;

    @Autowired
    private ZonePersistenceAdapter zonePersistenceAdapter;

    @Autowired
    private WarehouseR2dbcRepository warehouseR2dbcRepository;

    @Autowired
    private ZoneR2dbcRepository zoneR2dbcRepository;

    @AfterEach
    void cleanUp() {
        zoneR2dbcRepository.deleteAll().block();
        warehouseR2dbcRepository.deleteAll().block();
    }

    @Test
    void 구역을_저장하면_ID가_채번되고_구역코드와_상태가_정상적으로_왕복된다() {
        Long warehouseId = warehousePersistenceAdapter.save(warehouse().build())
                .map(w -> w.getWarehouseId())
                .block();
        Zone zone = zone().warehouseId(warehouseId).zoneCode(ZoneCode.D).zoneStatus(AvailabilityStatus.AVAILABLE).build();

        StepVerifier.create(
                        zonePersistenceAdapter.save(zone)
                                .flatMap(saved -> zonePersistenceAdapter.findById(saved.getZoneId()))
                )
                .assertNext(found -> {
                    assertThat(found.getZoneId()).isNotNull();
                    assertThat(found.getWarehouseId()).isEqualTo(warehouseId);
                    assertThat(found.getZoneCode()).isEqualTo(ZoneCode.D);
                    assertThat(found.getZoneStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
                })
                .verifyComplete();
    }

    @Test
    void 존재하지_않는_ID로_조회하면_빈_결과를_반환한다() {
        StepVerifier.create(zonePersistenceAdapter.findById(999_999L))
                .verifyComplete();
    }
}
