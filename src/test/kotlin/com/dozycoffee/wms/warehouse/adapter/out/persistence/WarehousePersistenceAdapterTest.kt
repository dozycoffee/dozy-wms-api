package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier

@DataR2dbcTest
@Import(R2dbcConfig::class, WarehousePersistenceAdapter::class)
class WarehousePersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @AfterEach
    fun cleanUp() {
        warehouseR2dbcRepository.deleteAll().block()
    }

    @Test
    fun `창고를 저장하면 ID가 채번되고 상태가 정상적으로 왕복된다`() {
        val warehouse: Warehouse = warehouse().warehouseStatus(AvailabilityStatus.AVAILABLE).build()

        StepVerifier.create(
            warehousePersistenceAdapter.save(warehouse)
                .flatMap { saved -> warehousePersistenceAdapter.findById(requireNotNull(saved.warehouseId)) }
        )
            .assertNext { found ->
                assertThat(found.warehouseId).isNotNull()
                assertThat(found.warehouseName).isEqualTo(warehouse.warehouseName)
                assertThat(found.address).isEqualTo(warehouse.address)
                assertThat(found.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
            }
            .verifyComplete()
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 빈 결과를 반환한다`() {
        StepVerifier.create(warehousePersistenceAdapter.findById(999_999L))
            .verifyComplete()
    }

    @Test
    fun `이미 저장된 창고를 다시 저장해도 생성 시각이 유지된다`() {
        val saved: Warehouse = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).block())
        val warehouseId: Long = requireNotNull(saved.warehouseId)
        val beforeUpdate: WarehouseEntity = requireNotNull(warehouseR2dbcRepository.findById(warehouseId).block())
        saved.deactivate()

        StepVerifier.create(
            warehousePersistenceAdapter.save(saved)
                .then(warehouseR2dbcRepository.findById(warehouseId))
        )
            .assertNext { afterUpdate ->
                assertThat(afterUpdate.warehouseStatus)
                    .isEqualTo(CommonCodes.toCode("WAREHOUSE_STATUS", AvailabilityStatus.UNAVAILABLE))
                assertThat(afterUpdate.createdAt).isEqualTo(beforeUpdate.createdAt)
                assertThat(afterUpdate.createdBy).isEqualTo(beforeUpdate.createdBy)
            }
            .verifyComplete()
    }
}
