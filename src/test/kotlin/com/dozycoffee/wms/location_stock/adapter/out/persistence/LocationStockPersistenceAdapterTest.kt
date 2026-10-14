package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.persistence.isDuplicateKey
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    SystemActorProvider::class,
    ProductPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class,
    LocationStockPersistenceAdapter::class
)
class LocationStockPersistenceAdapterTest {

    @Autowired
    private lateinit var locationStockPersistenceAdapter: LocationStockPersistenceAdapter

    @Autowired
    private lateinit var locationStockR2dbcRepository: LocationStockR2dbcRepository

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var locationPersistenceAdapter: LocationPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

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

    private suspend fun createLocation(): Long {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val zoneId: Long = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        return requireNotNull(locationPersistenceAdapter.save(location().zoneId(zoneId).build()).locationId)
    }

    private suspend fun createProduct(): Long {
        return requireNotNull(productPersistenceAdapter.save(product().build()).productId)
    }

    @Test
    fun `저장한 장부 행을 키로 잠그고 조회한다`() = runTest {
        val locationId = createLocation()
        val productId = createProduct()
        val saved = locationStockPersistenceAdapter.save(
            locationStock().locationId(locationId).productId(productId).lotNumber("L-1").quantity(12).build()
        )

        val found = locationStockPersistenceAdapter.findForUpdate(locationId, productId, "L-1", StockQualityStatus.NORMAL)

        assertThat(found).isNotNull
        assertThat(found?.locationStockId).isEqualTo(saved.locationStockId)
        assertThat(found?.quantity).isEqualTo(12)
    }

    @Test
    fun `키가 다르면 조회되지 않는다`() = runTest {
        val locationId = createLocation()
        val productId = createProduct()
        locationStockPersistenceAdapter.save(
            locationStock().locationId(locationId).productId(productId).lotNumber("L-1").build()
        )

        assertThat(locationStockPersistenceAdapter.findForUpdate(locationId, productId, "L-2", StockQualityStatus.NORMAL)).isNull()
        assertThat(locationStockPersistenceAdapter.findForUpdate(locationId, productId, "L-1", StockQualityStatus.DEFECTIVE)).isNull()
    }

    @Test
    fun `Lot 번호는 대소문자를 구분한다`() = runTest {
        val locationId = createLocation()
        val productId = createProduct()
        locationStockPersistenceAdapter.save(
            locationStock().locationId(locationId).productId(productId).lotNumber("lot-1").quantity(1).build()
        )
        locationStockPersistenceAdapter.save(
            locationStock().locationId(locationId).productId(productId).lotNumber("LOT-1").quantity(2).build()
        )

        assertThat(locationStockPersistenceAdapter.findAllByLocationId(locationId).toList()).hasSize(2)
        assertThat(
            locationStockPersistenceAdapter.findForUpdate(locationId, productId, "LOT-1", StockQualityStatus.NORMAL)?.quantity
        ).isEqualTo(2)
    }

    @Test
    fun `수정하면 수량이 갱신되고 생성 정보는 유지된다`() = runTest {
        val locationId = createLocation()
        val productId = createProduct()
        val saved = locationStockPersistenceAdapter.save(
            locationStock().locationId(locationId).productId(productId).quantity(10).build()
        )
        val createdAt = locationStockR2dbcRepository.findById(requireNotNull(saved.locationStockId))?.createdAt
        saved.decrease(10)

        locationStockPersistenceAdapter.save(saved)

        val reloaded = locationStockR2dbcRepository.findById(requireNotNull(saved.locationStockId))
        assertThat(reloaded?.quantity).isEqualTo(0)
        assertThat(reloaded?.createdAt).isEqualTo(createdAt)
    }

    @Test
    fun `같은 키를 두 번 저장하면 유니크 제약 위반이다`() = runTest {
        val locationId = createLocation()
        val productId = createProduct()
        locationStockPersistenceAdapter.save(
            locationStock().locationId(locationId).productId(productId).lotNumber("L-1").build()
        )

        assertThatThrownBy {
            kotlinx.coroutines.runBlocking {
                locationStockPersistenceAdapter.save(
                    locationStock().locationId(locationId).productId(productId).lotNumber("L-1").build()
                )
            }
        }.isInstanceOfSatisfying(DataIntegrityViolationException::class.java) {
            assertThat(it.isDuplicateKey()).isTrue()
        }
    }

    @Test
    fun `위치로 장부 행을 모두 조회한다`() = runTest {
        val locationId = createLocation()
        val productId = createProduct()
        locationStockPersistenceAdapter.save(locationStock().locationId(locationId).productId(productId).lotNumber("A").build())
        locationStockPersistenceAdapter.save(
            locationStock().locationId(locationId).productId(productId).lotNumber("A")
                .qualityStatus(StockQualityStatus.DEFECTIVE).build()
        )

        assertThat(locationStockPersistenceAdapter.findAllByLocationId(locationId).toList()).hasSize(2)
        assertThat(locationStockPersistenceAdapter.findAllByLocationId(locationId + 999).toList()).isEmpty()
    }

    private data class Site(val warehouseId: Long, val zoneId: Long)

    private suspend fun createSite(): Site {
        val warehouseId: Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val zoneId: Long = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        return Site(warehouseId, zoneId)
    }

    private suspend fun createLocationIn(zoneId: Long, code: String): Long =
        requireNotNull(locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode(code).build()).locationId)

    private suspend fun save(locationId: Long, productId: Long, lot: String, quality: StockQualityStatus, quantity: Int, held: Int = 0): LocationStock {
        val created = locationStockPersistenceAdapter.save(LocationStock.create(locationId, productId, lot, quality, quantity))
        if (held == 0) return created
        created.hold(held)
        return locationStockPersistenceAdapter.save(created)
    }

    @Test
    fun `점유 수량이 저장되고 ID로 한 번에 조회된다`() = runTest {
        val site = createSite()
        val productId = createProduct()
        val locationId = createLocationIn(site.zoneId, "A-01")
        val saved = save(locationId, productId, "L-1", StockQualityStatus.NORMAL, 10, held = 4)

        val found = locationStockPersistenceAdapter.findAllByIds(listOf(requireNotNull(saved.locationStockId)))

        assertThat(found.single().heldQuantity).isEqualTo(4)
        assertThat(found.single().availableQuantity).isEqualTo(6)
        assertThat(locationStockPersistenceAdapter.findByIdForUpdate(requireNotNull(saved.locationStockId))?.heldQuantity).isEqualTo(4)
        assertThat(locationStockPersistenceAdapter.findAllByIds(emptyList())).isEmpty()
    }

    @Test
    fun `창고의 같은 Lot 정상 행을 가용 수량이 많은 순으로 가져오고 가용이 없거나 다른 조건은 뺀다`() = runTest {
        val site = createSite()
        val other = createSite()
        val productId = createProduct()
        val a01 = createLocationIn(site.zoneId, "A-01")
        val a02 = createLocationIn(site.zoneId, "A-02")
        val a03 = createLocationIn(site.zoneId, "A-03")
        val a04 = createLocationIn(site.zoneId, "A-04")
        val otherLocation = createLocationIn(other.zoneId, "A-01")
        save(a01, productId, "L-1", StockQualityStatus.NORMAL, 10, held = 4)
        save(a02, productId, "L-1", StockQualityStatus.NORMAL, 6)
        save(a03, productId, "L-1", StockQualityStatus.NORMAL, 5, held = 5)
        save(a04, productId, "L-1", StockQualityStatus.DEFECTIVE, 9)
        save(a04, productId, "L-2", StockQualityStatus.NORMAL, 9)
        save(otherLocation, productId, "L-1", StockQualityStatus.NORMAL, 20)

        val rows = locationStockPersistenceAdapter.findAvailableForUpdate(site.warehouseId, productId, "L-1", StockQualityStatus.NORMAL)

        assertThat(rows.map { it.locationId }).containsExactly(a01, a02)
        assertThat(rows.map { it.availableQuantity }).containsExactly(6, 6)
    }

    @Test
    fun `가용 수량이 같으면 Location 코드 오름차순이다`() = runTest {
        val site = createSite()
        val productId = createProduct()
        val a02 = createLocationIn(site.zoneId, "A-02")
        val a01 = createLocationIn(site.zoneId, "A-01")
        save(a02, productId, "L-1", StockQualityStatus.NORMAL, 7)
        save(a01, productId, "L-1", StockQualityStatus.NORMAL, 7)

        val rows = locationStockPersistenceAdapter.findAvailableForUpdate(site.warehouseId, productId, "L-1", StockQualityStatus.NORMAL)

        assertThat(rows.map { it.locationId }).containsExactly(a01, a02)
    }

    @Test
    fun `수량이 0인 행도 Lot이 있는 것으로 본다`() = runTest {
        val site = createSite()
        val productId = createProduct()
        val locationId = createLocationIn(site.zoneId, "A-01")
        val row = save(locationId, productId, "L-1", StockQualityStatus.NORMAL, 3)
        row.decrease(3)
        locationStockPersistenceAdapter.save(row)

        assertThat(locationStockPersistenceAdapter.existsByProductIdAndLotNumber(productId, "L-1")).isTrue()
        assertThat(locationStockPersistenceAdapter.existsByProductIdAndLotNumber(productId, "L-9")).isFalse()
    }

    @Test
    fun `기준 시각 이후에 갱신된 행만 변경으로 본다`() = runTest {
        val site = createSite()
        val productId = createProduct()
        val locationId = createLocationIn(site.zoneId, "A-01")
        val id = requireNotNull(save(locationId, productId, "L-1", StockQualityStatus.NORMAL, 3).locationStockId)

        assertThat(locationStockPersistenceAdapter.existsUpdatedAfter(id, LocalDateTime.now().minusHours(1))).isTrue()
        assertThat(locationStockPersistenceAdapter.existsUpdatedAfter(id, LocalDateTime.now().plusHours(1))).isFalse()
    }
}
