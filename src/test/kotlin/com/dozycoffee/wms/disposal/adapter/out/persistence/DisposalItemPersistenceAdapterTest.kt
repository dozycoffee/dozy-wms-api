package com.dozycoffee.wms.disposal.adapter.out.persistence

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.fixture.DisposalItemTestBuilder.Companion.disposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalTestBuilder.Companion.disposal
import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.location_stock.adapter.out.persistence.LocationStockPersistenceAdapter
import com.dozycoffee.wms.location_stock.adapter.out.persistence.LocationStockR2dbcRepository
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
    DisposalPersistenceAdapter::class,
    DisposalItemPersistenceAdapter::class
)
class DisposalItemPersistenceAdapterTest {

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
    private lateinit var disposalPersistenceAdapter: DisposalPersistenceAdapter

    @Autowired
    private lateinit var disposalItemPersistenceAdapter: DisposalItemPersistenceAdapter

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
    private lateinit var disposalR2dbcRepository: DisposalR2dbcRepository

    @Autowired
    private lateinit var disposalItemR2dbcRepository: DisposalItemR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            disposalItemR2dbcRepository.deleteAll()
            disposalR2dbcRepository.deleteAll()
            locationStockR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
            locationR2dbcRepository.deleteAll()
            zoneR2dbcRepository.deleteAll()
            warehouseR2dbcRepository.deleteAll()
        }
    }

    private suspend fun createWarehouseId(): Long {
        return requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
    }

    private suspend fun createLocationId(warehouseId: Long): Long {
        val zoneId: Long = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        return requireNotNull(locationPersistenceAdapter.save(location().zoneId(zoneId).build()).locationId)
    }

    private var productSequence = 0

    private suspend fun createInventoryId(locationId: Long, quantity: Int = 5): Long {
        val productCode = "PRD-DISPOSAL-${productSequence++}"
        val productId = requireNotNull(productPersistenceAdapter.save(product().productCode(productCode).build()).productId)
        val saved = locationStockPersistenceAdapter.save(
            LocationStock.create(locationId, productId, "LOT-1", StockQualityStatus.DEFECTIVE, quantity)
        )
        return requireNotNull(saved.locationStockId)
    }

    private suspend fun createDisposalId(warehouseId: Long): Long {
        return requireNotNull(disposalPersistenceAdapter.save(disposal().warehouseId(warehouseId).build()).disposalId)
    }

    @Test
    fun `폐기 상품을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val locationId = createLocationId(warehouseId)
        val locationStockId = createInventoryId(locationId)
        val disposalId = createDisposalId(warehouseId)
        val newItem = disposalItem().disposalId(disposalId).locationStockId(locationStockId).quantity(5).reason(DisposalReason.EXPIRED).build()

        val saved = disposalItemPersistenceAdapter.save(newItem)
        val found = disposalItemPersistenceAdapter.findById(requireNotNull(saved.disposalItemId))

        assertThat(found).isNotNull
        assertThat(found?.disposalId).isEqualTo(disposalId)
        assertThat(found?.locationStockId).isEqualTo(locationStockId)
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
            disposalItem().disposalId(disposalId).locationStockId(createInventoryId(locationId)).build()
        )
        disposalItemPersistenceAdapter.save(
            disposalItem().disposalId(otherDisposalId).locationStockId(createInventoryId(locationId)).build()
        )

        val result = disposalItemPersistenceAdapter.findAllByDisposalId(disposalId).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().disposalItemId).isEqualTo(target.disposalItemId)
    }

    @Test
    fun `같은 위치 재고의 완료 전 폐기 신청 수량만 합산한다`() = runTest {
        val warehouseId = createWarehouseId()
        val locationId = createLocationId(warehouseId)
        val locationStockId = createInventoryId(locationId, 10)
        val requested = createDisposalId(warehouseId)
        val approved = requireNotNull(
            disposalPersistenceAdapter.save(disposal().warehouseId(warehouseId).status(DisposalStatus.REQUESTED).build()).disposalId
        )
        val completed = disposalPersistenceAdapter.save(disposal().warehouseId(warehouseId).build()).let {
            it.approve()
            it.complete()
            disposalPersistenceAdapter.save(it)
        }
        disposalItemPersistenceAdapter.save(disposalItem().disposalId(requested).locationStockId(locationStockId).quantity(3).build())
        disposalItemPersistenceAdapter.save(disposalItem().disposalId(approved).locationStockId(locationStockId).quantity(2).build())
        disposalItemPersistenceAdapter.save(
            disposalItem().disposalId(requireNotNull(completed.disposalId)).locationStockId(locationStockId).quantity(4).build()
        )

        assertThat(disposalItemPersistenceAdapter.sumPendingQuantityByLocationStockId(locationStockId)).isEqualTo(5)
        assertThat(disposalItemPersistenceAdapter.sumPendingQuantityByLocationStockId(999_999L)).isEqualTo(0)
    }

    @Test
    fun `이벤트 ID와 창고로 이미 만든 폐기 신청인지 확인한다`() = runTest {
        val warehouseId = createWarehouseId()
        disposalPersistenceAdapter.save(disposal().warehouseId(warehouseId).sourceEventId("event-1").build())

        assertThat(disposalPersistenceAdapter.existsBySourceEventIdAndWarehouseId("event-1", warehouseId)).isTrue()
        assertThat(disposalPersistenceAdapter.existsBySourceEventIdAndWarehouseId("event-1", warehouseId + 1)).isFalse()
        assertThat(disposalPersistenceAdapter.existsBySourceEventIdAndWarehouseId("event-2", warehouseId)).isFalse()
    }
}
