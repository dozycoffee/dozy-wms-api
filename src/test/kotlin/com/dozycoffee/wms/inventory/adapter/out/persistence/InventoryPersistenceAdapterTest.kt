package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
import com.dozycoffee.wms.inventory.application.port.`in`.InventorySortBy
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
import java.time.LocalDate

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    MockAccessScopeProvider::class,
    ProductPersistenceAdapter::class,
    LotPersistenceAdapter::class,
    InventoryPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class
)
class InventoryPersistenceAdapterTest {

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

    private fun createLocation(): Long {
        return createLocationInWarehouse().locationId
    }

    private data class CreatedLocation(val locationId: Long, val warehouseId: Long)

    private fun createLocationInWarehouse(): CreatedLocation {
        val warehouseId: Long = requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
        val zoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build())
                .map { requireNotNull(it.zoneId) }.block()
        )
        val locationId: Long = requireNotNull(
            locationPersistenceAdapter.save(location().zoneId(zoneId).build())
                .map { requireNotNull(it.locationId) }.block()
        )
        return CreatedLocation(locationId, warehouseId)
    }

    @Test
    fun `재고를 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val newInventory = inventory().productId(productId).lotId(lotId).locationId(locationId).quantity(20).build()

        val saved = inventoryPersistenceAdapter.save(newInventory)
        val found = inventoryPersistenceAdapter.findById(requireNotNull(saved.inventoryId))

        assertThat(found).isNotNull
        assertThat(found?.productId).isEqualTo(productId)
        assertThat(found?.lotId).isEqualTo(lotId)
        assertThat(found?.locationId).isEqualTo(locationId)
        assertThat(found?.quantity).isEqualTo(20)
        assertThat(found?.qualityStatus).isEqualTo(QualityStatus.NORMAL)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        val found = inventoryPersistenceAdapter.findById(999_999L)

        assertThat(found).isNull()
    }

    @Test
    fun `삭제된 재고는 findById로 조회되지 않는다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val saved = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).build()
        )
        val inventoryId = requireNotNull(saved.inventoryId)

        saved.delete("system")
        inventoryPersistenceAdapter.save(saved)

        assertThat(inventoryPersistenceAdapter.findById(inventoryId)).isNull()
    }

    @Test
    fun `위치와 품질 상태로 목록을 필터링한다`() = runTest {
        val locationId = createLocation()
        val otherLocationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val target = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).build()
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(otherLocationId).build()
        )

        val result = inventoryPersistenceAdapter.findAll(locationId, null, QualityStatus.NORMAL, null, null).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().inventoryId).isEqualTo(target.inventoryId)
    }

    @Test
    fun `수량 오름차순으로 정렬 조회한다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val larger = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).quantity(30).build()
        )
        val smaller = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).quantity(10).build()
        )

        val result = inventoryPersistenceAdapter.findAll(null, productId, null, InventorySortBy.QUANTITY, null).toList()

        assertThat(result.map { it.inventoryId }).containsExactly(smaller.inventoryId, larger.inventoryId)
    }

    @Test
    fun `유통기한 임박순으로 정렬 조회한다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val farLotId = requireNotNull(
            lotPersistenceAdapter.save(
                lot().productId(productId).lotNumber("LOT-FAR").expirationDate(LocalDate.of(2027, 12, 31)).build()
            ).lotId
        )
        val nearLotId = requireNotNull(
            lotPersistenceAdapter.save(
                lot().productId(productId).lotNumber("LOT-NEAR").expirationDate(LocalDate.of(2026, 12, 31)).build()
            ).lotId
        )
        val far = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(farLotId).locationId(locationId).build()
        )
        val near = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(nearLotId).locationId(locationId).build()
        )

        val result = inventoryPersistenceAdapter.findAll(null, productId, null, InventorySortBy.EXPIRATION_DATE, null).toList()

        assertThat(result.map { it.inventoryId }).containsExactly(near.inventoryId, far.inventoryId)
    }

    @Test
    fun `입고일 오름차순으로 정렬 조회한다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val firstInbound = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).build()
        )
        val secondInbound = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).build()
        )

        val result = inventoryPersistenceAdapter.findAll(null, productId, null, InventorySortBy.INBOUND_DATE, null).toList()

        assertThat(result.map { it.inventoryId }).containsExactly(firstInbound.inventoryId, secondInbound.inventoryId)
    }

    @Test
    fun `warehouseIds를 지정하면 해당 창고 소속 재고만 조회한다`() = runTest {
        val locationInWarehouseA = createLocationInWarehouse()
        val locationInWarehouseB = createLocationInWarehouse()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val target = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationInWarehouseA.locationId).build()
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationInWarehouseB.locationId).build()
        )

        val result = inventoryPersistenceAdapter
            .findAll(null, null, null, null, listOf(locationInWarehouseA.warehouseId))
            .toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().inventoryId).isEqualTo(target.inventoryId)
    }

    @Test
    fun `warehouseIds가 빈 리스트이면 전체 재고를 조회한다`() = runTest {
        val locationInWarehouseA = createLocationInWarehouse()
        val locationInWarehouseB = createLocationInWarehouse()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationInWarehouseA.locationId).build()
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationInWarehouseB.locationId).build()
        )

        val result = inventoryPersistenceAdapter.findAll(null, null, null, null, emptyList()).toList()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `Lot ID로 재고 목록을 조회한다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val otherLotId = requireNotNull(
            lotPersistenceAdapter.save(lot().productId(productId).lotNumber("LOT-OTHER").build()).lotId
        )
        val target = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).build()
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(otherLotId).locationId(locationId).build()
        )

        val result = inventoryPersistenceAdapter.findAllByLotId(lotId).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().inventoryId).isEqualTo(target.inventoryId)
    }
}
