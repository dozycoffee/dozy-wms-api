package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.location_stock.adapter.out.persistence.LocationStockPersistenceAdapter
import com.dozycoffee.wms.location_stock.adapter.out.persistence.LocationStockR2dbcRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import com.dozycoffee.wms.stock_audit.fixture.StockAuditItemTestBuilder.Companion.stockAuditItem
import com.dozycoffee.wms.stock_audit.fixture.StockAuditTestBuilder.Companion.stockAudit
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationPersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
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
    StockAuditPersistenceAdapter::class,
    StockAuditItemPersistenceAdapter::class
)
class StockAuditItemPersistenceAdapterTest {

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
    private lateinit var stockAuditPersistenceAdapter: StockAuditPersistenceAdapter

    @Autowired
    private lateinit var stockAuditItemPersistenceAdapter: StockAuditItemPersistenceAdapter

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

    @Autowired
    private lateinit var stockAuditR2dbcRepository: StockAuditR2dbcRepository

    @Autowired
    private lateinit var stockAuditItemR2dbcRepository: StockAuditItemR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            stockAuditItemR2dbcRepository.deleteAll()
            stockAuditR2dbcRepository.deleteAll()
            locationStockR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
            locationR2dbcRepository.deleteAll()
            zoneR2dbcRepository.deleteAll()
            warehouseR2dbcRepository.deleteAll()
        }
    }

    private suspend fun createWarehouseAndZone(): Pair<Long, Long> {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val zoneId: Long = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        return warehouseId to zoneId
    }

    private suspend fun createInventory(zoneId: Long): Long {
        val locationId: Long = requireNotNull(locationPersistenceAdapter.save(location().zoneId(zoneId).build()).locationId)
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        return requireNotNull(
            locationStockPersistenceAdapter.save(
                LocationStock.create(locationId, productId, "LOT-1", StockQualityStatus.NORMAL, 20)
            ).locationStockId
        )
    }

    private suspend fun createStockAudit(warehouseId: Long, zoneId: Long): Long {
        return requireNotNull(
            stockAuditPersistenceAdapter.save(stockAudit().warehouseId(warehouseId).zoneId(zoneId).build()).stockAuditId
        )
    }

    private suspend fun createStockAuditAndInventory(): Pair<Long, Long> {
        val (warehouseId, zoneId) = createWarehouseAndZone()
        val locationStockId = createInventory(zoneId)
        val stockAuditId = createStockAudit(warehouseId, zoneId)
        return stockAuditId to locationStockId
    }

    @Test
    fun `실사 항목을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val (stockAuditId, locationStockId) = createStockAuditAndInventory()
        val newItem: StockAuditItem =
            stockAuditItem().stockAuditId(stockAuditId).locationStockId(locationStockId).snapshotQuantity(20).build()

        val saved = stockAuditItemPersistenceAdapter.save(newItem)
        val found = stockAuditItemPersistenceAdapter.findById(requireNotNull(saved.stockAuditItemId))

        assertThat(found).isNotNull
        assertThat(found?.stockAuditId).isEqualTo(stockAuditId)
        assertThat(found?.locationStockId).isEqualTo(locationStockId)
        assertThat(found?.snapshotQuantity).isEqualTo(20)
        assertThat(found?.isCounted).isFalse()
    }

    @Test
    fun `실측 수량을 입력한 뒤 재저장하면 카운트 결과가 반영된다`() = runTest {
        val (stockAuditId, locationStockId) = createStockAuditAndInventory()
        val saved = stockAuditItemPersistenceAdapter.save(
            stockAuditItem().stockAuditId(stockAuditId).locationStockId(locationStockId).snapshotQuantity(20).build()
        )

        saved.count(18)
        stockAuditItemPersistenceAdapter.save(saved)
        val found = stockAuditItemPersistenceAdapter.findById(requireNotNull(saved.stockAuditItemId))

        assertThat(found?.countedQuantity).isEqualTo(18)
        assertThat(found?.discrepancy).isEqualTo(-2)
    }

    @Test
    fun `실사 ID로 항목 목록을 조회한다`() = runTest {
        val (warehouseId, zoneId) = createWarehouseAndZone()
        val locationStockId = createInventory(zoneId)
        val stockAuditId = createStockAudit(warehouseId, zoneId)
        val otherStockAuditId = createStockAudit(warehouseId, zoneId)
        val target = stockAuditItemPersistenceAdapter.save(
            stockAuditItem().stockAuditId(stockAuditId).locationStockId(locationStockId).snapshotQuantity(20).build()
        )
        stockAuditItemPersistenceAdapter.save(
            stockAuditItem().stockAuditId(otherStockAuditId).locationStockId(locationStockId).snapshotQuantity(5).build()
        )

        val result = stockAuditItemPersistenceAdapter.findAllByStockAuditId(stockAuditId).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().stockAuditItemId).isEqualTo(target.stockAuditItemId)
    }
}
