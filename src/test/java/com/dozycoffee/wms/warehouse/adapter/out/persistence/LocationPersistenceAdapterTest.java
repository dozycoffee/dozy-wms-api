package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.global.config.R2dbcConfig;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.location;
import static com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.warehouse;
import static com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.zone;
import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({R2dbcConfig.class, WarehousePersistenceAdapter.class, ZonePersistenceAdapter.class, LocationPersistenceAdapter.class})
class LocationPersistenceAdapterTest {

    @Autowired
    private WarehousePersistenceAdapter warehousePersistenceAdapter;

    @Autowired
    private ZonePersistenceAdapter zonePersistenceAdapter;

    @Autowired
    private LocationPersistenceAdapter locationPersistenceAdapter;

    @Autowired
    private WarehouseR2dbcRepository warehouseR2dbcRepository;

    @Autowired
    private ZoneR2dbcRepository zoneR2dbcRepository;

    @Autowired
    private LocationR2dbcRepository locationR2dbcRepository;

    @AfterEach
    void cleanUp() {
        locationR2dbcRepository.deleteAll().block();
        zoneR2dbcRepository.deleteAll().block();
        warehouseR2dbcRepository.deleteAll().block();
    }

    @Test
    void 위치를_저장하면_ID가_채번되고_점유량과_상태가_정상적으로_왕복된다() {
        Long warehouseId = warehousePersistenceAdapter.save(warehouse().build())
                .map(w -> w.getWarehouseId())
                .block();
        Long zoneId = zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build())
                .map(z -> z.getZoneId())
                .block();
        Location newLocation = location().zoneId(zoneId).locationCode("A-01").maxCapacity(70).build();
        Location saved = locationPersistenceAdapter.save(newLocation).block();
        saved.occupy(30);

        StepVerifier.create(
                        locationPersistenceAdapter.save(saved)
                                .flatMap(updated -> locationPersistenceAdapter.findById(updated.getLocationId()))
                )
                .assertNext(found -> {
                    assertThat(found.getLocationId()).isNotNull();
                    assertThat(found.getZoneId()).isEqualTo(zoneId);
                    assertThat(found.getLocationCode().value()).isEqualTo("A-01");
                    assertThat(found.getMaxCapacity().value()).isEqualTo(70);
                    assertThat(found.getUsedCapacity()).isEqualTo(30);
                    assertThat(found.getLocationStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
                })
                .verifyComplete();
    }

    @Test
    void 존재하지_않는_ID로_조회하면_빈_결과를_반환한다() {
        StepVerifier.create(locationPersistenceAdapter.findById(999_999L))
                .verifyComplete();
    }
}
