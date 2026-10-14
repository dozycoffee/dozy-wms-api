package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.location_stock.adapter.out.persistence.LocationStockPersistenceAdapter
import com.dozycoffee.wms.location_stock.adapter.out.persistence.LocationStockR2dbcRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocationPick
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationPersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.Companion.location
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.context.annotation.Import
import java.time.LocalDate

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    SystemActorProvider::class,
    ProductPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    OutboundPersistenceAdapter::class,
    OutboundItemPersistenceAdapter::class,
    OutboundAllocationPersistenceAdapter::class,
    OutboundAllocationPickPersistenceAdapter::class,
    LocationStockPersistenceAdapter::class,
    LocationPersistenceAdapter::class,
    ZonePersistenceAdapter::class
)
class OutboundAllocationPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var outboundPersistenceAdapter: OutboundPersistenceAdapter

    @Autowired
    private lateinit var outboundItemPersistenceAdapter: OutboundItemPersistenceAdapter

    @Autowired
    private lateinit var outboundAllocationPersistenceAdapter: OutboundAllocationPersistenceAdapter

    @Autowired
    private lateinit var outboundAllocationR2dbcRepository: OutboundAllocationR2dbcRepository

    @Autowired
    private lateinit var outboundAllocationPickPersistenceAdapter: OutboundAllocationPickPersistenceAdapter

    @Autowired
    private lateinit var outboundAllocationPickR2dbcRepository: OutboundAllocationPickR2dbcRepository

    @Autowired
    private lateinit var locationStockPersistenceAdapter: LocationStockPersistenceAdapter

    @Autowired
    private lateinit var locationStockR2dbcRepository: LocationStockR2dbcRepository

    @Autowired
    private lateinit var locationPersistenceAdapter: LocationPersistenceAdapter

    @Autowired
    private lateinit var locationR2dbcRepository: LocationR2dbcRepository

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var outboundItemR2dbcRepository: OutboundItemR2dbcRepository

    @Autowired
    private lateinit var outboundR2dbcRepository: OutboundR2dbcRepository

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            outboundAllocationPickR2dbcRepository.deleteAll()
            outboundAllocationR2dbcRepository.deleteAll()
            outboundItemR2dbcRepository.deleteAll()
            outboundR2dbcRepository.deleteAll()
            locationStockR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
            locationR2dbcRepository.deleteAll()
            zoneR2dbcRepository.deleteAll()
            warehouseR2dbcRepository.deleteAll()
        }
    }

    private suspend fun createWarehouseId(): Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)

    private suspend fun createOutboundItemId(warehouseId: Long, productId: Long, reservationId: Long): Long {
        val outbound = outboundPersistenceAdapter.save(outbound().warehouseId(warehouseId).reservationId(reservationId).build())
        val item = outboundItemPersistenceAdapter.save(OutboundItem.create(outbound.outboundId, productId, 10))
        return requireNotNull(item.outboundItemId)
    }

    @Test
    fun `출고 상품의 할당을 저장하고 출고 상품 ID로 모아서 조회한다`() = runTest {
        val warehouseId = createWarehouseId()
        val productId = productPersistenceAdapter.save(product().build()).productId
        val itemId = createOutboundItemId(warehouseId, productId, 7L)
        outboundAllocationPersistenceAdapter.save(OutboundAllocation.create(itemId, 55L, "A", LocalDate.of(2027, 1, 1), 4))
        outboundAllocationPersistenceAdapter.save(OutboundAllocation.create(itemId, 56L, "B", null, 6))

        val found = outboundAllocationPersistenceAdapter.findAllByOutboundItemIds(listOf(itemId))

        assertThat(found).hasSize(2)
        assertThat(found.map { it.inventoryId }).containsExactlyInAnyOrder(55L, 56L)
        assertThat(found.first { it.inventoryId == 55L }.expirationDate).isEqualTo(LocalDate.of(2027, 1, 1))
        assertThat(found.first { it.inventoryId == 56L }.expirationDate).isNull()
    }

    @Test
    fun `피킹 수량 갱신이 저장된다`() = runTest {
        val warehouseId = createWarehouseId()
        val productId = productPersistenceAdapter.save(product().build()).productId
        val itemId = createOutboundItemId(warehouseId, productId, 7L)
        val saved = outboundAllocationPersistenceAdapter.save(OutboundAllocation.create(itemId, 55L, "A", null, 4))

        saved.pick(3)
        outboundAllocationPersistenceAdapter.save(saved)

        val found = outboundAllocationPersistenceAdapter.findAllByOutboundItemIds(listOf(itemId))
        assertThat(found.single().pickedQuantity).isEqualTo(3)
    }

    @Test
    fun `같은 창고에서 같은 예약의 출고는 둘 만들 수 없고 예약으로 조회한다`() = runTest {
        val warehouseId = createWarehouseId()
        outboundPersistenceAdapter.save(outbound().warehouseId(warehouseId).reservationId(7L).build())

        val found = outboundPersistenceAdapter.findByWarehouseIdAndReservationId(warehouseId, 7L)

        assertThat(found?.reservationId).isEqualTo(7L)
        assertThat(found?.channel).isEqualTo("OMS")
        assertThatThrownBy {
            runBlocking { outboundPersistenceAdapter.save(outbound().warehouseId(warehouseId).reservationId(7L).build()) }
        }.isInstanceOf(DataIntegrityViolationException::class.java)
        assertThat(outboundPersistenceAdapter.findByWarehouseIdAndReservationId(warehouseId, 8L)).isNull()
    }

    @Test
    fun `할당을 삭제하면 조회되지 않는다`() = runTest {
        val warehouseId = createWarehouseId()
        val productId = productPersistenceAdapter.save(product().build()).productId
        val itemId = createOutboundItemId(warehouseId, productId, 7L)
        val saved = outboundAllocationPersistenceAdapter.save(OutboundAllocation.create(itemId, 55L, "A", null, 4))

        outboundAllocationPersistenceAdapter.delete(saved)

        assertThat(outboundAllocationPersistenceAdapter.findAllByOutboundItemIds(listOf(itemId))).isEmpty()
    }

    @Test
    fun `재할당 횟수가 저장되고 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val saved = outboundPersistenceAdapter.save(outbound().warehouseId(warehouseId).reservationId(7L).build())
        assertThat(saved.reallocationCount).isEqualTo(0)

        saved.markReallocated()
        outboundPersistenceAdapter.save(saved)

        assertThat(outboundPersistenceAdapter.findById(requireNotNull(saved.outboundId))?.reallocationCount).isEqualTo(1)
    }

    @Test
    fun `피킹 계획을 저장하고 할당 ID로 모아 조회하고 할당별로 지운다`() = runTest {
        val warehouseId = createWarehouseId()
        val zoneId = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        val locationId = requireNotNull(locationPersistenceAdapter.save(location().zoneId(zoneId).build()).locationId)
        val productId = productPersistenceAdapter.save(product().build()).productId
        val stockId = requireNotNull(
            locationStockPersistenceAdapter.save(LocationStock.create(locationId, productId, "A", StockQualityStatus.NORMAL, 10)).locationStockId
        )
        val itemId = createOutboundItemId(warehouseId, productId, 7L)
        val first = outboundAllocationPersistenceAdapter.save(OutboundAllocation.create(itemId, 55L, "A", null, 4))
        val second = outboundAllocationPersistenceAdapter.save(OutboundAllocation.create(itemId, 56L, "B", null, 2))
        val firstId = requireNotNull(first.outboundAllocationId)
        val secondId = requireNotNull(second.outboundAllocationId)
        outboundAllocationPickPersistenceAdapter.save(OutboundAllocationPick.create(firstId, stockId, 4))
        outboundAllocationPickPersistenceAdapter.save(OutboundAllocationPick.create(secondId, stockId, 2))

        val picks = outboundAllocationPickPersistenceAdapter.findAllByOutboundAllocationIds(listOf(firstId, secondId))
        assertThat(picks.map { it.outboundAllocationId to it.quantity }).containsExactlyInAnyOrder(firstId to 4, secondId to 2)

        outboundAllocationPickPersistenceAdapter.deleteAllByOutboundAllocationId(firstId)

        assertThat(outboundAllocationPickPersistenceAdapter.findAllByOutboundAllocationIds(listOf(firstId, secondId)).map { it.outboundAllocationId })
            .containsExactly(secondId)
        assertThat(outboundAllocationPickPersistenceAdapter.findAllByOutboundAllocationIds(emptyList())).isEmpty()
    }
}
