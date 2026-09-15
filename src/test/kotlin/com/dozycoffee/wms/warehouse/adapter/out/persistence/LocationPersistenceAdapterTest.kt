package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.model.Location
import com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.Companion.location
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier

@DataR2dbcTest
@Import(R2dbcConfig::class, WarehousePersistenceAdapter::class, ZonePersistenceAdapter::class, LocationPersistenceAdapter::class)
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
    fun cleanUp() {
        locationR2dbcRepository.deleteAll().block()
        zoneR2dbcRepository.deleteAll().block()
        warehouseR2dbcRepository.deleteAll().block()
    }

    @Test
    fun `위치를 저장하면 ID가 채번되고 점유량과 상태가 정상적으로 왕복된다`() {
        val warehouseId: Long = requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
        val zoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).map { requireNotNull(it.zoneId) }.block()
        )
        val newLocation: Location = location().zoneId(zoneId).locationCode("A-01").maxCapacity(70).build()
        val saved: Location = requireNotNull(locationPersistenceAdapter.save(newLocation).block())
        saved.occupy(30)

        StepVerifier.create(
            locationPersistenceAdapter.save(saved)
                .flatMap { updated -> locationPersistenceAdapter.findById(requireNotNull(updated.locationId)) }
        )
            .assertNext { found ->
                assertThat(found.locationId).isNotNull()
                assertThat(found.zoneId).isEqualTo(zoneId)
                assertThat(found.locationCode.value).isEqualTo("A-01")
                assertThat(found.maxCapacity.value).isEqualTo(70)
                assertThat(found.usedCapacity).isEqualTo(30)
                assertThat(found.locationStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
            }
            .verifyComplete()
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 빈 결과를 반환한다`() {
        StepVerifier.create(locationPersistenceAdapter.findById(999_999L))
            .verifyComplete()
    }

    @Test
    fun `Zone ID로 조회하면 해당 Zone에 속한 위치만 반환한다`() {
        val warehouseId: Long = requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
        val zoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).map { requireNotNull(it.zoneId) }.block()
        )
        val otherZoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).zoneCode(ZoneCode.B).build())
                .map { requireNotNull(it.zoneId) }.block()
        )
        locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode("A-01").maxCapacity(70).build()).block()
        locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode("A-02").maxCapacity(60).build()).block()
        locationPersistenceAdapter.save(location().zoneId(otherZoneId).locationCode("B-01").maxCapacity(60).build()).block()

        StepVerifier.create(locationPersistenceAdapter.findByZoneId(zoneId).collectList())
            .assertNext { found ->
                assertThat(found).hasSize(2)
                assertThat(found.map { it.locationCode.value }).containsExactlyInAnyOrder("A-01", "A-02")
            }
            .verifyComplete()
    }
}
