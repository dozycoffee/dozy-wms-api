package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.global.config.R2dbcConfig;
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.WorkArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.warehouse;
import static com.dozycoffee.wms.warehouse.fixture.WorkAreaTestBuilder.workArea;
import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({R2dbcConfig.class, WarehousePersistenceAdapter.class, WorkAreaPersistenceAdapter.class})
class WorkAreaPersistenceAdapterTest {

    @Autowired
    private WarehousePersistenceAdapter warehousePersistenceAdapter;

    @Autowired
    private WorkAreaPersistenceAdapter workAreaPersistenceAdapter;

    @Autowired
    private WarehouseR2dbcRepository warehouseR2dbcRepository;

    @Autowired
    private WorkAreaR2dbcRepository workAreaR2dbcRepository;

    @AfterEach
    void cleanUp() {
        workAreaR2dbcRepository.deleteAll().block();
        warehouseR2dbcRepository.deleteAll().block();
    }

    @Test
    void 작업구역을_저장하면_ID가_채번되고_점유량과_상태가_정상적으로_왕복된다() {
        Long warehouseId = warehousePersistenceAdapter.save(warehouse().build())
                .map(w -> w.getWarehouseId())
                .block();
        WorkArea newWorkArea = workArea().warehouseId(warehouseId).areaCode(AreaCode.OUTBOUND).build();
        WorkArea saved = workAreaPersistenceAdapter.save(newWorkArea).block();
        saved.occupy(10);

        StepVerifier.create(
                        workAreaPersistenceAdapter.save(saved)
                                .flatMap(updated -> workAreaPersistenceAdapter.findById(updated.getWorkAreaId()))
                )
                .assertNext(found -> {
                    assertThat(found.getWorkAreaId()).isNotNull();
                    assertThat(found.getWarehouseId()).isEqualTo(warehouseId);
                    assertThat(found.getAreaCode()).isEqualTo(AreaCode.OUTBOUND);
                    assertThat(found.getUsedCapacity()).isEqualTo(10);
                    assertThat(found.getWorkAreaStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
                })
                .verifyComplete();
    }

    @Test
    void 존재하지_않는_ID로_조회하면_빈_결과를_반환한다() {
        StepVerifier.create(workAreaPersistenceAdapter.findById(999_999L))
                .verifyComplete();
    }
}
