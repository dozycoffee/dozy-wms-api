package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.global.config.R2dbcConfig;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.Warehouse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.warehouse;
import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({R2dbcConfig.class, WarehousePersistenceAdapter.class})
class WarehousePersistenceAdapterTest {

    @Autowired
    private WarehousePersistenceAdapter warehousePersistenceAdapter;

    @Autowired
    private WarehouseR2dbcRepository warehouseR2dbcRepository;

    @AfterEach
    void cleanUp() {
        warehouseR2dbcRepository.deleteAll().block();
    }

    @Test
    void 창고를_저장하면_ID가_채번되고_상태가_정상적으로_왕복된다() {
        Warehouse warehouse = warehouse().warehouseStatus(AvailabilityStatus.AVAILABLE).build();

        StepVerifier.create(
                        warehousePersistenceAdapter.save(warehouse)
                                .flatMap(saved -> warehousePersistenceAdapter.findById(saved.getWarehouseId()))
                )
                .assertNext(found -> {
                    assertThat(found.getWarehouseId()).isNotNull();
                    assertThat(found.getWarehouseName()).isEqualTo(warehouse.getWarehouseName());
                    assertThat(found.getAddress()).isEqualTo(warehouse.getAddress());
                    assertThat(found.getWarehouseStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
                })
                .verifyComplete();
    }

    @Test
    void 존재하지_않는_ID로_조회하면_빈_결과를_반환한다() {
        StepVerifier.create(warehousePersistenceAdapter.findById(999_999L))
                .verifyComplete();
    }

    @Test
    void 이미_저장된_창고를_다시_저장해도_생성_시각이_유지된다() {
        Warehouse saved = warehousePersistenceAdapter.save(warehouse().build()).block();
        Long warehouseId = saved.getWarehouseId();
        WarehouseEntity beforeUpdate = warehouseR2dbcRepository.findById(warehouseId).block();
        saved.deactivate();

        StepVerifier.create(
                        warehousePersistenceAdapter.save(saved)
                                .then(warehouseR2dbcRepository.findById(warehouseId))
                )
                .assertNext(afterUpdate -> {
                    assertThat(afterUpdate.getWarehouseStatus())
                            .isEqualTo(CommonCodes.toCode("WAREHOUSE_STATUS", AvailabilityStatus.UNAVAILABLE));
                    assertThat(afterUpdate.getCreatedAt()).isEqualTo(beforeUpdate.getCreatedAt());
                    assertThat(afterUpdate.getCreatedBy()).isEqualTo(beforeUpdate.getCreatedBy());
                })
                .verifyComplete();
    }
}
