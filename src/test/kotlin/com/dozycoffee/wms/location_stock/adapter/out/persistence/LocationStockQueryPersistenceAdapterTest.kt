package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.location_stock.application.port.`in`.query.LocationStockQuery
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
    LocationStockQueryPersistenceAdapter::class
)
class LocationStockQueryPersistenceAdapterTest {

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
    private lateinit var queryAdapter: LocationStockQueryPersistenceAdapter

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

    private suspend fun createStock(locationId: Long, productId: Long, lot: String, quality: StockQualityStatus, quantity: Int, held: Int = 0, emptied: Boolean = false) {
        val stock = LocationStock.create(locationId, productId, lot, quality, quantity)
        if (held > 0) stock.hold(held)
        if (emptied) stock.decrease(quantity)
        locationStockPersistenceAdapter.save(stock)
    }

    private var productSequence: Int = 0

    private suspend fun newProduct(): Long {
        productSequence += 1
        return requireNotNull(productPersistenceAdapter.save(product().productCode("PRD-Q$productSequence").build()).productId)
    }

    @Test
    fun `장부 행에 Location과 Zone 정보를 붙여 반환한다`() = runTest {
        val zoneA = createZone(ZoneCode.A)
        val location = createLocation(zoneA.zoneId, "A-01", 70, 20)
        val productId = newProduct()
        createStock(location, productId, "LOT-1", StockQualityStatus.NORMAL, 20, held = 5)

        val result = queryAdapter.findAll(LocationStockQuery()).toList()

        assertThat(result).hasSize(1)
        val row = result[0]
        assertThat(row.warehouseId).isEqualTo(zoneA.warehouseId)
        assertThat(row.zoneId).isEqualTo(zoneA.zoneId)
        assertThat(row.zoneCode).isEqualTo(ZoneCode.A)
        assertThat(row.locationId).isEqualTo(location)
        assertThat(row.locationCode).isEqualTo("A-01")
        assertThat(row.productId).isEqualTo(productId)
        assertThat(row.lotNumber).isEqualTo("LOT-1")
        assertThat(row.qualityStatus).isEqualTo(StockQualityStatus.NORMAL)
        assertThat(row.quantity).isEqualTo(20)
        assertThat(row.heldQuantity).isEqualTo(5)
        assertThat(row.availableQuantity).isEqualTo(15)
        assertThat(row.updatedAt).isNotNull()
    }

    @Test
    fun `수량 0 행은 includeEmpty일 때만 반환한다`() = runTest {
        val zoneA = createZone(ZoneCode.A)
        val location = createLocation(zoneA.zoneId, "A-01", 70, 0)
        val productId = newProduct()
        createStock(location, productId, "LOT-1", StockQualityStatus.NORMAL, 5, emptied = true)
        createStock(location, productId, "LOT-2", StockQualityStatus.NORMAL, 8)

        assertThat(queryAdapter.findAll(LocationStockQuery()).toList().map { it.lotNumber }).containsExactly("LOT-2")
        assertThat(queryAdapter.findAll(LocationStockQuery(includeEmpty = true)).toList().map { it.lotNumber })
            .containsExactly("LOT-1", "LOT-2")
    }

    @Test
    fun `창고 Zone Location 상품 품질 상태 조건으로 거른다`() = runTest {
        val zoneA = createZone(ZoneCode.A)
        val zoneB = createZone(ZoneCode.B)
        val a1 = createLocation(zoneA.zoneId, "A-01", 70, 0)
        val a2 = createLocation(zoneA.zoneId, "A-02", 70, 0)
        val b1 = createLocation(zoneB.zoneId, "B-01", 70, 0)
        val product1 = newProduct()
        val product2 = newProduct()
        createStock(a1, product1, "LOT-1", StockQualityStatus.NORMAL, 10)
        createStock(a2, product1, "LOT-1", StockQualityStatus.DEFECTIVE, 3)
        createStock(a2, product2, "LOT-2", StockQualityStatus.NORMAL, 4)
        createStock(b1, product2, "LOT-2", StockQualityStatus.NORMAL, 6)

        assertThat(queryAdapter.findAll(LocationStockQuery(warehouseIds = listOf(zoneA.warehouseId))).toList()).hasSize(3)
        assertThat(queryAdapter.findAll(LocationStockQuery(zoneId = zoneB.zoneId)).toList().map { it.locationCode }).containsExactly("B-01")
        assertThat(queryAdapter.findAll(LocationStockQuery(locationId = a2)).toList()).hasSize(2)
        assertThat(queryAdapter.findAll(LocationStockQuery(productId = product2)).toList()).hasSize(2)
        assertThat(queryAdapter.findAll(LocationStockQuery(qualityStatus = StockQualityStatus.DEFECTIVE)).toList().map { it.quantity })
            .containsExactly(3)
    }

    @Test
    fun `여러 조건은 모두 만족하는 행만 반환하고 창고 조건은 Zone 조건으로 우회할 수 없다`() = runTest {
        val zoneA = createZone(ZoneCode.A)
        val zoneB = createZone(ZoneCode.B)
        val a1 = createLocation(zoneA.zoneId, "A-01", 70, 0)
        val b1 = createLocation(zoneB.zoneId, "B-01", 70, 0)
        val productId = newProduct()
        createStock(a1, productId, "LOT-1", StockQualityStatus.NORMAL, 10)
        createStock(b1, productId, "LOT-1", StockQualityStatus.NORMAL, 6)

        val result = queryAdapter.findAll(LocationStockQuery(warehouseIds = listOf(zoneA.warehouseId), zoneId = zoneB.zoneId)).toList()

        assertThat(result).isEmpty()
    }

    @Test
    fun `창고 Location 코드 상품 Lot 순서로 정렬한다`() = runTest {
        val zoneA = createZone(ZoneCode.A)
        val a1 = createLocation(zoneA.zoneId, "A-01", 70, 0)
        val a2 = createLocation(zoneA.zoneId, "A-02", 70, 0)
        val productId = newProduct()
        createStock(a2, productId, "LOT-1", StockQualityStatus.NORMAL, 1)
        createStock(a1, productId, "LOT-2", StockQualityStatus.NORMAL, 1)
        createStock(a1, productId, "LOT-1", StockQualityStatus.NORMAL, 1)

        val result = queryAdapter.findAll(LocationStockQuery()).toList()

        assertThat(result.map { "${it.locationCode}/${it.lotNumber}" }).containsExactly("A-01/LOT-1", "A-01/LOT-2", "A-02/LOT-1")
    }
}
