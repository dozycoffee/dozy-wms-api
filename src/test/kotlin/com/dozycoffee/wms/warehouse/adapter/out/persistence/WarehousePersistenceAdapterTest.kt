package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import

@DataR2dbcTest
@Import(R2dbcConfig::class, SystemActorProvider::class, WarehousePersistenceAdapter::class)
class WarehousePersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @AfterEach
    fun cleanUp() = runTest {
        warehouseR2dbcRepository.deleteAll()
    }

    @Test
    fun `창고를 저장하면 ID가 채번되고 상태가 정상적으로 왕복된다`() = runTest {
        val warehouse: Warehouse = warehouse().warehouseStatus(AvailabilityStatus.AVAILABLE).build()

        val saved = warehousePersistenceAdapter.save(warehouse)
        val found = requireNotNull(warehousePersistenceAdapter.findById(requireNotNull(saved.warehouseId)))

        assertThat(found.warehouseId).isNotNull()
        assertThat(found.warehouseName).isEqualTo(warehouse.warehouseName)
        assertThat(found.address).isEqualTo(warehouse.address)
        assertThat(found.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 빈 결과를 반환한다`() = runTest {
        assertThat(warehousePersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `이미 저장된 창고를 다시 저장해도 생성 시각이 유지된다`() = runTest {
        val saved: Warehouse = warehousePersistenceAdapter.save(warehouse().build())
        val warehouseId: Long = requireNotNull(saved.warehouseId)
        val beforeUpdate: WarehouseEntity = requireNotNull(warehouseR2dbcRepository.findById(warehouseId))
        saved.deactivate()

        warehousePersistenceAdapter.save(saved)
        val afterUpdate = requireNotNull(warehouseR2dbcRepository.findById(warehouseId))

        assertThat(afterUpdate.warehouseStatus).isEqualTo(AvailabilityStatus.UNAVAILABLE.name)
        assertThat(afterUpdate.createdAt).isEqualTo(beforeUpdate.createdAt)
        assertThat(afterUpdate.createdBy).isEqualTo(beforeUpdate.createdBy)
    }
}
