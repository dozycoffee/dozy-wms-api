package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.fixture.InventoryTestBuilder.Companion.inventory
import com.dozycoffee.wms.inventory.fixture.LotTestBuilder.Companion.lot
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationPersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
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
@Import(
    R2dbcConfig::class,
    ProductPersistenceAdapter::class,
    LotPersistenceAdapter::class,
    InventoryPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class,
    ZoneInventorySummaryPersistenceAdapter::class
)
class ZoneInventorySummaryPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var lotPersistenceAdapter: LotPersistenceAdapter

    @Autowired
    private lateinit var inventoryPersistenceAdapter: InventoryPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var locationPersistenceAdapter: LocationPersistenceAdapter

    @Autowired
    private lateinit var zoneInventorySummaryPersistenceAdapter: ZoneInventorySummaryPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var lotR2dbcRepository: LotR2dbcRepository

    @Autowired
    private lateinit var inventoryR2dbcRepository: InventoryR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var locationR2dbcRepository: LocationR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            inventoryR2dbcRepository.deleteAll()
            lotR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
        }
        locationR2dbcRepository.deleteAll().block()
        zoneR2dbcRepository.deleteAll().block()
        warehouseR2dbcRepository.deleteAll().block()
    }

    private data class CreatedZone(val zoneId: Long, val warehouseId: Long)

    private fun createZone(zoneCode: ZoneCode): CreatedZone {
        val warehouseId: Long = requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
        val zoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).zoneCode(zoneCode).build())
                .map { requireNotNull(it.zoneId) }.block()
        )
        return CreatedZone(zoneId, warehouseId)
    }

    private fun createLocation(zoneId: Long, locationCode: String, maxCapacity: Int, usedCapacity: Int): Long {
        val locationId: Long = requireNotNull(
            locationPersistenceAdapter.save(
                location().zoneId(zoneId).locationCode(locationCode).maxCapacity(maxCapacity).build()
            ).map { requireNotNull(it.locationId) }.block()
        )
        if (usedCapacity > 0) {
            locationPersistenceAdapter.save(
                location().locationId(locationId).zoneId(zoneId).locationCode(locationCode)
                    .maxCapacity(maxCapacity).usedCapacity(usedCapacity).build()
            ).block()
        }
        return locationId
    }

    @Test
    fun `Zone별 Capacity와 품질상태별 재고 수량을 집계한다`() = runTest {
        val zoneA = createZone(ZoneCode.A)
        val zoneB = createZone(ZoneCode.B)
        val zoneALocation1 = createLocation(zoneA.zoneId, "A-01", 70, 20)
        val zoneALocation2 = createLocation(zoneA.zoneId, "A-02", 60, 10)
        createLocation(zoneB.zoneId, "B-01", 60, 0)

        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(zoneALocation1).quantity(20).build()
        )
        val defective = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(zoneALocation2).quantity(10).build()
        )
        defective.markDefective()
        inventoryPersistenceAdapter.save(defective)

        val result = zoneInventorySummaryPersistenceAdapter.findAll().toList()

        val zoneASummary = result.first { it.zoneId == zoneA.zoneId }
        assertThat(zoneASummary.zoneCode).isEqualTo(ZoneCode.A)
        assertThat(zoneASummary.warehouseId).isEqualTo(zoneA.warehouseId)
        assertThat(zoneASummary.maxCapacity).isEqualTo(130)
        assertThat(zoneASummary.usedCapacity).isEqualTo(30)
        assertThat(zoneASummary.quantityByQualityStatus)
            .containsEntry(QualityStatus.NORMAL, 20)
            .containsEntry(QualityStatus.DEFECTIVE, 10)

        val zoneBSummary = result.first { it.zoneId == zoneB.zoneId }
        assertThat(zoneBSummary.warehouseId).isEqualTo(zoneB.warehouseId)
        assertThat(zoneBSummary.maxCapacity).isEqualTo(60)
        assertThat(zoneBSummary.usedCapacity).isEqualTo(0)
        assertThat(zoneBSummary.quantityByQualityStatus).isEmpty()
    }

    @Test
    fun `Location이 없는 Zone은 Capacity 0으로 집계된다`() = runTest {
        val zone = createZone(ZoneCode.C)

        val result = zoneInventorySummaryPersistenceAdapter.findAll().toList()

        val summary = result.first { it.zoneId == zone.zoneId }
        assertThat(summary.warehouseId).isEqualTo(zone.warehouseId)
        assertThat(summary.maxCapacity).isEqualTo(0)
        assertThat(summary.usedCapacity).isEqualTo(0)
        assertThat(summary.quantityByQualityStatus).isEmpty()
    }
}
