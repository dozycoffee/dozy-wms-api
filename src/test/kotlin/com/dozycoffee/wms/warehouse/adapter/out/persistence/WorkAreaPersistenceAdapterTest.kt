package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.fixture.WorkAreaTestBuilder.Companion.workArea
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import

@DataR2dbcTest
@Import(R2dbcConfig::class, SystemActorProvider::class, WarehousePersistenceAdapter::class, WorkAreaPersistenceAdapter::class)
class WorkAreaPersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var workAreaPersistenceAdapter: WorkAreaPersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var workAreaR2dbcRepository: WorkAreaR2dbcRepository

    @AfterEach
    fun cleanUp() = runTest {
        workAreaR2dbcRepository.deleteAll()
        warehouseR2dbcRepository.deleteAll()
    }

    @Test
    fun `작업구역을 저장하면 ID가 채번되고 점유량과 상태가 정상적으로 왕복된다`() = runTest {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val newWorkArea: WorkArea = workArea().warehouseId(warehouseId).areaCode(AreaCode.OUTBOUND).build()
        val saved: WorkArea = workAreaPersistenceAdapter.save(newWorkArea)
        saved.occupy(10)

        val updated = workAreaPersistenceAdapter.save(saved)
        val found = requireNotNull(workAreaPersistenceAdapter.findById(requireNotNull(updated.workAreaId)))

        assertThat(found.workAreaId).isNotNull()
        assertThat(found.warehouseId).isEqualTo(warehouseId)
        assertThat(found.areaCode).isEqualTo(AreaCode.OUTBOUND)
        assertThat(found.usedCapacity).isEqualTo(10)
        assertThat(found.workAreaStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 빈 결과를 반환한다`() = runTest {
        assertThat(workAreaPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `창고 ID와 구역타입으로 조회하면 해당 작업구역을 반환한다`() = runTest {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        workAreaPersistenceAdapter.save(workArea().warehouseId(warehouseId).areaCode(AreaCode.INBOUND).build())

        val found = requireNotNull(workAreaPersistenceAdapter.findByWarehouseIdAndAreaCode(warehouseId, AreaCode.INBOUND))

        assertThat(found.areaCode).isEqualTo(AreaCode.INBOUND)
    }

    @Test
    fun `창고에 없는 구역타입으로 조회하면 빈 결과를 반환한다`() = runTest {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)

        assertThat(workAreaPersistenceAdapter.findByWarehouseIdAndAreaCode(warehouseId, AreaCode.OUTBOUND)).isNull()
    }
}
