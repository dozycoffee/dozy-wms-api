package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.model.Location
import com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.Companion.location
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import

@DataR2dbcTest
@Import(R2dbcConfig::class, SystemActorProvider::class, WarehousePersistenceAdapter::class, ZonePersistenceAdapter::class, LocationPersistenceAdapter::class)
class LocationPersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var locationPersistenceAdapter: LocationPersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var locationR2dbcRepository: LocationR2dbcRepository

    @AfterEach
    fun cleanUp() = runTest {
        locationR2dbcRepository.deleteAll()
        zoneR2dbcRepository.deleteAll()
        warehouseR2dbcRepository.deleteAll()
    }

    @Test
    fun `위치를 저장하면 ID가 채번되고 점유량과 상태가 정상적으로 왕복된다`() = runTest {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val zoneId: Long = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        val newLocation: Location = location().zoneId(zoneId).locationCode("A-01").maxCapacity(70).build()
        val saved: Location = locationPersistenceAdapter.save(newLocation)
        saved.occupy(30)

        val updated = locationPersistenceAdapter.save(saved)
        val found = requireNotNull(locationPersistenceAdapter.findById(requireNotNull(updated.locationId)))

        assertThat(found.locationId).isNotNull()
        assertThat(found.zoneId).isEqualTo(zoneId)
        assertThat(found.locationCode.value).isEqualTo("A-01")
        assertThat(found.maxCapacity.value).isEqualTo(70)
        assertThat(found.usedCapacity).isEqualTo(30)
        assertThat(found.locationStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 빈 결과를 반환한다`() = runTest {
        assertThat(locationPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `Zone ID로 조회하면 해당 Zone에 속한 위치만 반환한다`() = runTest {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val zoneId: Long = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        val otherZoneId: Long = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).zoneCode(ZoneCode.B).build()).zoneId)
        locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode("A-01").maxCapacity(70).build())
        locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode("A-02").maxCapacity(60).build())
        locationPersistenceAdapter.save(location().zoneId(otherZoneId).locationCode("B-01").maxCapacity(60).build())

        val found = locationPersistenceAdapter.findByZoneId(zoneId).toList()

        assertThat(found).hasSize(2)
        assertThat(found.map { it.locationCode.value }).containsExactlyInAnyOrder("A-01", "A-02")
    }
}
