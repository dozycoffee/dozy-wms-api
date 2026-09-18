package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.fixture.InventoryHistoryTestBuilder.Companion.inventoryHistory
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
import java.time.LocalDateTime

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    ProductPersistenceAdapter::class,
    LotPersistenceAdapter::class,
    InventoryPersistenceAdapter::class,
    InventoryHistoryPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class
)
class InventoryHistoryPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var lotPersistenceAdapter: LotPersistenceAdapter

    @Autowired
    private lateinit var inventoryPersistenceAdapter: InventoryPersistenceAdapter

    @Autowired
    private lateinit var inventoryHistoryPersistenceAdapter: InventoryHistoryPersistenceAdapter

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
    private lateinit var inventoryHistoryR2dbcRepository: InventoryHistoryR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var locationR2dbcRepository: LocationR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            inventoryHistoryR2dbcRepository.deleteAll()
            inventoryR2dbcRepository.deleteAll()
            lotR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
        }
        locationR2dbcRepository.deleteAll().block()
        zoneR2dbcRepository.deleteAll().block()
        warehouseR2dbcRepository.deleteAll().block()
    }

    private suspend fun createInventory(productCode: String = "PRD-0001"): Long {
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
        val productId =
            requireNotNull(productPersistenceAdapter.save(product().productCode(productCode).build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val saved = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).quantity(50).build()
        )
        return requireNotNull(saved.inventoryId)
    }

    @Test
    fun `이력을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val inventoryId = createInventory()
        val newHistory = inventoryHistory()
            .inventoryId(inventoryId).historyType(InventoryHistoryType.INBOUND).quantityChange(30).referenceId(100L)
            .build()

        val saved = inventoryHistoryPersistenceAdapter.save(newHistory)

        assertThat(saved.inventoryHistoryId).isNotNull()
        assertThat(saved.inventoryId).isEqualTo(inventoryId)
        assertThat(saved.historyType).isEqualTo(InventoryHistoryType.INBOUND)
        assertThat(saved.quantityChange).isEqualTo(30)
        assertThat(saved.referenceId).isEqualTo(100L)
    }

    @Test
    fun `음수 변화량(출고 폐기)도 정상적으로 저장된다`() = runTest {
        val inventoryId = createInventory()
        val newHistory = inventoryHistory()
            .inventoryId(inventoryId).historyType(InventoryHistoryType.OUTBOUND).quantityChange(-10).referenceId(200L)
            .build()

        val saved = inventoryHistoryPersistenceAdapter.save(newHistory)

        assertThat(saved.quantityChange).isEqualTo(-10)
        assertThat(saved.historyType).isEqualTo(InventoryHistoryType.OUTBOUND)
    }

    @Test
    fun `inventoryId로 필터링하면 해당 재고의 이력만 최신순으로 반환한다`() = runTest {
        val inventoryId1 = createInventory("PRD-0001")
        val inventoryId2 = createInventory("PRD-0002")
        inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId1).historyType(InventoryHistoryType.INBOUND)
                .quantityChange(10).referenceId(1L).build()
        )
        val second = inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId1).historyType(InventoryHistoryType.OUTBOUND)
                .quantityChange(-5).referenceId(2L).build()
        )
        inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId2).historyType(InventoryHistoryType.INBOUND)
                .quantityChange(20).referenceId(3L).build()
        )

        val result = inventoryHistoryPersistenceAdapter.findAll(inventoryId1, null, null, null).toList()

        assertThat(result).hasSize(2)
        assertThat(result).allMatch { it.inventoryId == inventoryId1 }
        assertThat(result.first().inventoryHistoryId).isEqualTo(second.inventoryHistoryId)
    }

    @Test
    fun `historyType으로 필터링하면 해당 유형의 이력만 반환한다`() = runTest {
        val inventoryId = createInventory()
        inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId).historyType(InventoryHistoryType.INBOUND)
                .quantityChange(10).referenceId(1L).build()
        )
        inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId).historyType(InventoryHistoryType.OUTBOUND)
                .quantityChange(-5).referenceId(2L).build()
        )

        val result = inventoryHistoryPersistenceAdapter.findAll(null, InventoryHistoryType.OUTBOUND, null, null).toList()

        assertThat(result).hasSize(1)
        assertThat(result[0].historyType).isEqualTo(InventoryHistoryType.OUTBOUND)
    }

    @Test
    fun `기간으로 필터링하면 범위 밖 이력은 제외된다`() = runTest {
        val inventoryId = createInventory()
        inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId).historyType(InventoryHistoryType.INBOUND)
                .quantityChange(10).referenceId(1L).build()
        )

        val within = inventoryHistoryPersistenceAdapter.findAll(
            inventoryId, null, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1)
        ).toList()
        val outside = inventoryHistoryPersistenceAdapter.findAll(
            inventoryId, null, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)
        ).toList()

        assertThat(within).hasSize(1)
        assertThat(outside).isEmpty()
    }

    @Test
    fun `최근 이력을 limit건만 최신순으로 반환한다`() = runTest {
        val inventoryId = createInventory()
        val first = inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId).historyType(InventoryHistoryType.INBOUND)
                .quantityChange(10).referenceId(1L).build()
        )
        val second = inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId).historyType(InventoryHistoryType.OUTBOUND)
                .quantityChange(-5).referenceId(2L).build()
        )
        val third = inventoryHistoryPersistenceAdapter.save(
            inventoryHistory().inventoryId(inventoryId).historyType(InventoryHistoryType.OUTBOUND)
                .quantityChange(-3).referenceId(3L).build()
        )

        val result = inventoryHistoryPersistenceAdapter.findRecentByInventoryId(inventoryId, 2).toList()

        assertThat(result.map { it.inventoryHistoryId }).containsExactly(third.inventoryHistoryId, second.inventoryHistoryId)
        assertThat(result).noneMatch { it.inventoryHistoryId == first.inventoryHistoryId }
    }
}
