package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
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
    SystemActorProvider::class,
    ProductPersistenceAdapter::class,
    LocationStockPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class,
    ZoneStockSummaryPersistenceAdapter::class
)
class ZoneStockSummaryPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var locationStockPersistenceAdapter: LocationStockPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var locationPersistenceAdapter: LocationPersistenceAdapter

    @Autowired
    private lateinit var zoneStockSummaryPersistenceAdapter: ZoneStockSummaryPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var locationStockR2dbcRepository: LocationStockR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var locationR2dbcRepository: LocationR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            locationStockR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
            locationR2dbcRepository.deleteAll()
            zoneR2dbcRepository.deleteAll()
            warehouseR2dbcRepository.deleteAll()
        }
    }

    private data class CreatedZone(val zoneId: Long, val warehouseId: Long)

    private suspend fun createZone(zoneCode: ZoneCode): CreatedZone {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val zoneId: Long = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).zoneCode(zoneCode).build()).zoneId)
        return CreatedZone(zoneId, warehouseId)
    }

    private suspend fun createLocation(zoneId: Long, locationCode: String, maxCapacity: Int, usedCapacity: Int): Long {
        val locationId: Long = requireNotNull(locationPersistenceAdapter.save(
                location().zoneId(zoneId).locationCode(locationCode).maxCapacity(maxCapacity).build()
            ).locationId)
        if (usedCapacity > 0) {
            locationPersistenceAdapter.save(
                location().locationId(locationId).zoneId(zoneId).locationCode(locationCode)
                    .maxCapacity(maxCapacity).usedCapacity(usedCapacity).build()
            )
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
        locationStockPersistenceAdapter.save(LocationStock.create(zoneALocation1, productId, "LOT-1", StockQualityStatus.NORMAL, 20))
        locationStockPersistenceAdapter.save(LocationStock.create(zoneALocation2, productId, "LOT-1", StockQualityStatus.DEFECTIVE, 10))

        val result = zoneStockSummaryPersistenceAdapter.findAll(null).toList()

        val zoneASummary = result.first { it.zoneId == zoneA.zoneId }
        assertThat(zoneASummary.zoneCode).isEqualTo(ZoneCode.A)
        assertThat(zoneASummary.warehouseId).isEqualTo(zoneA.warehouseId)
        assertThat(zoneASummary.maxCapacity).isEqualTo(130)
        assertThat(zoneASummary.usedCapacity).isEqualTo(30)
        assertThat(zoneASummary.quantityByQualityStatus)
            .containsEntry(StockQualityStatus.NORMAL, 20)
            .containsEntry(StockQualityStatus.DEFECTIVE, 10)

        val zoneBSummary = result.first { it.zoneId == zoneB.zoneId }
        assertThat(zoneBSummary.warehouseId).isEqualTo(zoneB.warehouseId)
        assertThat(zoneBSummary.maxCapacity).isEqualTo(60)
        assertThat(zoneBSummary.usedCapacity).isEqualTo(0)
        assertThat(zoneBSummary.quantityByQualityStatus).isEmpty()
    }

    @Test
    fun `Location이 없는 Zone은 Capacity 0으로 집계된다`() = runTest {
        val zone = createZone(ZoneCode.C)

        val result = zoneStockSummaryPersistenceAdapter.findAll(null).toList()

        val summary = result.first { it.zoneId == zone.zoneId }
        assertThat(summary.warehouseId).isEqualTo(zone.warehouseId)
        assertThat(summary.maxCapacity).isEqualTo(0)
        assertThat(summary.usedCapacity).isEqualTo(0)
        assertThat(summary.quantityByQualityStatus).isEmpty()
    }

    @Test
    fun `warehouseIds를 지정하면 해당 창고의 Zone만 반환한다`() = runTest {
        val zoneA = createZone(ZoneCode.A)
        val zoneB = createZone(ZoneCode.B)
        createLocation(zoneA.zoneId, "A-01", 70, 20)
        createLocation(zoneB.zoneId, "B-01", 60, 10)

        val result = zoneStockSummaryPersistenceAdapter.findAll(listOf(zoneA.warehouseId)).toList()

        assertThat(result).hasSize(1)
        assertThat(result[0].zoneId).isEqualTo(zoneA.zoneId)
        assertThat(result[0].warehouseId).isEqualTo(zoneA.warehouseId)
    }

    @Test
    fun `warehouseIds가 빈 리스트이면 전체 Zone을 반환한다`() = runTest {
        val zoneA = createZone(ZoneCode.A)
        val zoneB = createZone(ZoneCode.B)

        val result = zoneStockSummaryPersistenceAdapter.findAll(emptyList()).toList()

        assertThat(result.map { it.zoneId }).contains(zoneA.zoneId, zoneB.zoneId)
    }
}
