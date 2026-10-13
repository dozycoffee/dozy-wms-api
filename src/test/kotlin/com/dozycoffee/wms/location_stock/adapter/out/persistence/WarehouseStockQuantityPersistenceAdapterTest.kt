package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.location_stock.application.port.`in`.result.WarehouseStockQuantityResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.fixture.LocationStockTestBuilder.Companion.locationStock
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationPersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.Companion.location
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
@Import(
    R2dbcConfig::class,
    SystemActorProvider::class,
    ProductPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class,
    LocationStockPersistenceAdapter::class,
    WarehouseStockQuantityPersistenceAdapter::class
)
class WarehouseStockQuantityPersistenceAdapterTest {

    @Autowired
    private lateinit var adapter: WarehouseStockQuantityPersistenceAdapter

    @Autowired
    private lateinit var locationStockPersistenceAdapter: LocationStockPersistenceAdapter

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var locationPersistenceAdapter: LocationPersistenceAdapter

    @Autowired
    private lateinit var locationStockR2dbcRepository: LocationStockR2dbcRepository

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var locationR2dbcRepository: LocationR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

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

    private suspend fun createWarehouse(): Long =
        requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)

    private suspend fun createLocations(warehouseId: Long, count: Int): List<Long> {
        val zoneId = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        return List(count) { index ->
            requireNotNull(
                locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode("A-0${index + 1}").build()).locationId
            )
        }
    }

    private var productSequence: Int = 0

    private suspend fun createProduct(): Long {
        productSequence += 1
        return requireNotNull(
            productPersistenceAdapter.save(product().productCode("PRD-%04d".format(productSequence)).build()).productId
        )
    }

    private suspend fun stock(
        locationId: Long,
        productId: Long,
        lotNumber: String,
        quality: StockQualityStatus,
        quantity: Int
    ) {
        locationStockPersistenceAdapter.save(
            locationStock().locationId(locationId).productId(productId).lotNumber(lotNumber)
                .qualityStatus(quality).quantity(quantity).build()
        )
    }

    @Test
    fun `창고의 여러 Location 수량을 상품 x Lot x 품질 상태로 합산한다`() = runTest {
        val warehouseId = createWarehouse()
        val (locationA, locationB) = createLocations(warehouseId, 2)
        val productId = createProduct()
        stock(locationA, productId, "L-1", StockQualityStatus.NORMAL, 10)
        stock(locationB, productId, "L-1", StockQualityStatus.NORMAL, 5)
        stock(locationA, productId, "L-1", StockQualityStatus.DEFECTIVE, 2)
        stock(locationB, productId, "L-2", StockQualityStatus.NORMAL, 7)

        val result = adapter.sumByWarehouse(warehouseId, null, null)

        assertThat(result).containsExactly(
            WarehouseStockQuantityResult(productId, "L-1", StockQualityStatus.DEFECTIVE, 2),
            WarehouseStockQuantityResult(productId, "L-1", StockQualityStatus.NORMAL, 15),
            WarehouseStockQuantityResult(productId, "L-2", StockQualityStatus.NORMAL, 7)
        )
    }

    @Test
    fun `다른 창고의 수량은 합산하지 않는다`() = runTest {
        val warehouseA = createWarehouse()
        val warehouseB = createWarehouse()
        val (locationA) = createLocations(warehouseA, 1)
        val (locationB) = createLocations(warehouseB, 1)
        val productId = createProduct()
        stock(locationA, productId, "L-1", StockQualityStatus.NORMAL, 10)
        stock(locationB, productId, "L-1", StockQualityStatus.NORMAL, 99)

        assertThat(adapter.sumByWarehouse(warehouseA, null, null).single().quantity).isEqualTo(10)
        assertThat(adapter.sumByWarehouse(warehouseB, null, null).single().quantity).isEqualTo(99)
    }

    @Test
    fun `수량이 0인 항목은 포함하지 않는다`() = runTest {
        val warehouseId = createWarehouse()
        val (locationId) = createLocations(warehouseId, 1)
        val productId = createProduct()
        stock(locationId, productId, "L-1", StockQualityStatus.NORMAL, 5)
        val emptied = requireNotNull(
            locationStockPersistenceAdapter.findForUpdate(locationId, productId, "L-1", StockQualityStatus.NORMAL)
        )
        emptied.decrease(5)
        locationStockPersistenceAdapter.save(emptied)
        stock(locationId, productId, "L-2", StockQualityStatus.NORMAL, 3)

        val result = adapter.sumByWarehouse(warehouseId, null, null)

        assertThat(result.map { it.lotNumber }).containsExactly("L-2")
    }

    @Test
    fun `상품과 Lot 번호로 좁힐 수 있고 Lot 번호는 대소문자를 구분한다`() = runTest {
        val warehouseId = createWarehouse()
        val (locationId) = createLocations(warehouseId, 1)
        val productA = createProduct()
        val productB = createProduct()
        stock(locationId, productA, "lot-1", StockQualityStatus.NORMAL, 1)
        stock(locationId, productA, "LOT-1", StockQualityStatus.NORMAL, 2)
        stock(locationId, productB, "LOT-1", StockQualityStatus.NORMAL, 4)

        assertThat(adapter.sumByWarehouse(warehouseId, productA, null)).hasSize(2)
        assertThat(adapter.sumByWarehouse(warehouseId, null, "LOT-1").map { it.quantity }).containsExactlyInAnyOrder(2, 4)
        assertThat(adapter.sumByWarehouse(warehouseId, productA, "LOT-1").single().quantity).isEqualTo(2)
        assertThat(adapter.sumByWarehouse(warehouseId, productA, "none")).isEmpty()
    }

    @Test
    fun `재고가 없는 창고는 빈 목록이다`() = runTest {
        val warehouseId = createWarehouse()

        assertThat(adapter.sumByWarehouse(warehouseId, null, null)).isEmpty()
    }
}
