package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.model.Zone
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import

@DataR2dbcTest
@Import(R2dbcConfig::class, SystemActorProvider::class, WarehousePersistenceAdapter::class, ZonePersistenceAdapter::class)
class ZonePersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @AfterEach
    fun cleanUp() = runTest {
        zoneR2dbcRepository.deleteAll()
        warehouseR2dbcRepository.deleteAll()
    }

    @Test
    fun `구역을 저장하면 ID가 채번되고 구역코드와 상태가 정상적으로 왕복된다`() = runTest {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val zone: Zone = zone().warehouseId(warehouseId).zoneCode(ZoneCode.D).zoneStatus(AvailabilityStatus.AVAILABLE).build()

        val saved = zonePersistenceAdapter.save(zone)
        val found = requireNotNull(zonePersistenceAdapter.findById(requireNotNull(saved.zoneId)))

        assertThat(found.zoneId).isNotNull()
        assertThat(found.warehouseId).isEqualTo(warehouseId)
        assertThat(found.zoneCode).isEqualTo(ZoneCode.D)
        assertThat(found.zoneStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 빈 결과를 반환한다`() = runTest {
        assertThat(zonePersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `창고 ID와 구역코드로 조회하면 해당 구역을 반환한다`() = runTest {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        zonePersistenceAdapter.save(zone().warehouseId(warehouseId).zoneCode(ZoneCode.A).build())

        val found = requireNotNull(zonePersistenceAdapter.findByWarehouseIdAndZoneCode(warehouseId, ZoneCode.A))

        assertThat(found.zoneCode).isEqualTo(ZoneCode.A)
    }

    @Test
    fun `창고에 없는 구역코드로 조회하면 빈 결과를 반환한다`() = runTest {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)

        assertThat(zonePersistenceAdapter.findByWarehouseIdAndZoneCode(warehouseId, ZoneCode.B)).isNull()
    }
}
