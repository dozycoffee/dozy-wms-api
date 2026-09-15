package com.dozycoffee.wms.disposal.adapter.out.persistence

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.fixture.DisposalItemTestBuilder.Companion.disposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalTestBuilder.Companion.disposal
import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.inventory.adapter.out.persistence.InventoryPersistenceAdapter
import com.dozycoffee.wms.inventory.adapter.out.persistence.InventoryR2dbcRepository
import com.dozycoffee.wms.inventory.adapter.out.persistence.LotPersistenceAdapter
import com.dozycoffee.wms.inventory.adapter.out.persistence.LotR2dbcRepository
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

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    ProductPersistenceAdapter::class,
    LotPersistenceAdapter::class,
    InventoryPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class,
    DisposalPersistenceAdapter::class,
    DisposalItemPersistenceAdapter::class
)
class DisposalItemPersistenceAdapterTest {

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
    private lateinit var disposalPersistenceAdapter: DisposalPersistenceAdapter

    @Autowired
    private lateinit var disposalItemPersistenceAdapter: DisposalItemPersistenceAdapter

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

    @Autowired
    private lateinit var disposalR2dbcRepository: DisposalR2dbcRepository

    @Autowired
    private lateinit var disposalItemR2dbcRepository: DisposalItemR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            disposalItemR2dbcRepository.deleteAll()
            disposalR2dbcRepository.deleteAll()
            inventoryR2dbcRepository.deleteAll()
            lotR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
        }
        locationR2dbcRepository.deleteAll().block()
        zoneR2dbcRepository.deleteAll().block()
        warehouseR2dbcRepository.deleteAll().block()
    }

    private fun createWarehouseId(): Long {
        return requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
    }

    private fun createLocationId(warehouseId: Long): Long {
        val zoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build())
                .map { requireNotNull(it.zoneId) }.block()
        )
        return requireNotNull(
            locationPersistenceAdapter.save(location().zoneId(zoneId).build())
                .map { requireNotNull(it.locationId) }.block()
        )
    }

    private var productSequence = 0

    private suspend fun createInventoryId(locationId: Long, quantity: Int = 5): Long {
        val productCode = "PRD-DISPOSAL-${productSequence++}"
        val productId = requireNotNull(productPersistenceAdapter.save(product().productCode(productCode).build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val saved = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).quantity(quantity).build()
        )
        return requireNotNull(saved.inventoryId)
    }

    private suspend fun createDisposalId(warehouseId: Long): Long {
        return requireNotNull(disposalPersistenceAdapter.save(disposal().warehouseId(warehouseId).build()).disposalId)
    }

    @Test
    fun `폐기 상품을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val locationId = createLocationId(warehouseId)
        val inventoryId = createInventoryId(locationId)
        val disposalId = createDisposalId(warehouseId)
        val newItem = disposalItem().disposalId(disposalId).inventoryId(inventoryId).quantity(5).reason(DisposalReason.EXPIRED).build()

        val saved = disposalItemPersistenceAdapter.save(newItem)
        val found = disposalItemPersistenceAdapter.findById(requireNotNull(saved.disposalItemId))

        assertThat(found).isNotNull
        assertThat(found?.disposalId).isEqualTo(disposalId)
        assertThat(found?.inventoryId).isEqualTo(inventoryId)
        assertThat(found?.quantity).isEqualTo(5)
        assertThat(found?.reason).isEqualTo(DisposalReason.EXPIRED)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(disposalItemPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `폐기 ID로 목록을 조회한다`() = runTest {
        val warehouseId = createWarehouseId()
        val locationId = createLocationId(warehouseId)
        val disposalId = createDisposalId(warehouseId)
        val otherDisposalId = createDisposalId(warehouseId)
        val target = disposalItemPersistenceAdapter.save(
            disposalItem().disposalId(disposalId).inventoryId(createInventoryId(locationId)).build()
        )
        disposalItemPersistenceAdapter.save(
            disposalItem().disposalId(otherDisposalId).inventoryId(createInventoryId(locationId)).build()
        )

        val result = disposalItemPersistenceAdapter.findAllByDisposalId(disposalId).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().disposalItemId).isEqualTo(target.disposalItemId)
    }
}
