package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.persistence.isDuplicateKey
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
}
