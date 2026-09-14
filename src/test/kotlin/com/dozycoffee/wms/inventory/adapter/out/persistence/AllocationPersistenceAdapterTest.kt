package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.model.Allocation
import com.dozycoffee.wms.inventory.fixture.AllocationTestBuilder.Companion.allocation
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
import kotlinx.coroutines.runBlocking
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
    ProductPersistenceAdapter::class,
    LotPersistenceAdapter::class,
    InventoryPersistenceAdapter::class,
    AllocationPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class
)
class AllocationPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var lotPersistenceAdapter: LotPersistenceAdapter

    @Autowired
    private lateinit var inventoryPersistenceAdapter: InventoryPersistenceAdapter

    @Autowired
    private lateinit var allocationPersistenceAdapter: AllocationPersistenceAdapter

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
    private lateinit var allocationR2dbcRepository: AllocationR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var locationR2dbcRepository: LocationR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            allocationR2dbcRepository.deleteAll()
            inventoryR2dbcRepository.deleteAll()
            lotR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
        }
        locationR2dbcRepository.deleteAll().block()
        zoneR2dbcRepository.deleteAll().block()
        warehouseR2dbcRepository.deleteAll().block()
    }

    private suspend fun createInventory(): Long {
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
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val saved = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).quantity(50).build()
        )
        return requireNotNull(saved.inventoryId)
    }

    @Test
    fun `점유를 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val inventoryId = createInventory()
        val newAllocation = allocation().inventoryId(inventoryId).referenceId(100L).quantity(10).build()

        val saved = allocationPersistenceAdapter.save(newAllocation)
        val found = allocationPersistenceAdapter.findById(requireNotNull(saved.allocationId))

        assertThat(found).isNotNull
        assertThat(found?.inventoryId).isEqualTo(inventoryId)
        assertThat(found?.referenceType).isEqualTo(AllocationReferenceType.OUTBOUND)
        assertThat(found?.quantity).isEqualTo(10)
        assertThat(found?.status).isEqualTo(AllocationStatus.HELD)
    }

    @Test
    fun `같은 참조로 HELD 상태 점유가 이미 있으면 findHeld로 조회된다`() = runTest {
        val inventoryId = createInventory()
        val saved = allocationPersistenceAdapter.save(
            allocation().inventoryId(inventoryId).referenceId(100L).quantity(10).build()
        )

        val found = allocationPersistenceAdapter.findHeld(inventoryId, AllocationReferenceType.OUTBOUND, 100L)

        assertThat(found?.allocationId).isEqualTo(saved.allocationId)
    }

    @Test
    fun `같은 참조로 중복 점유를 저장하면 유니크 제약을 위반한다 (ADR-0008 멱등성)`() = runTest {
        val inventoryId = createInventory()
        allocationPersistenceAdapter.save(allocation().inventoryId(inventoryId).referenceId(100L).quantity(10).build())

        assertThatThrownBy {
            runBlocking {
                allocationPersistenceAdapter.save(
                    allocation().inventoryId(inventoryId).referenceId(100L).quantity(5).build()
                )
            }
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `RELEASED 상태로 종결된 뒤에는 같은 참조로 다시 점유할 수 있다`() = runTest {
        val inventoryId = createInventory()
        val first: Allocation = allocationPersistenceAdapter.save(
            allocation().inventoryId(inventoryId).referenceId(100L).quantity(10).build()
        )
        first.release()
        allocationPersistenceAdapter.save(first)

        val second = allocationPersistenceAdapter.save(
            allocation().inventoryId(inventoryId).referenceId(100L).quantity(5).build()
        )

        assertThat(second.status).isEqualTo(AllocationStatus.HELD)
    }
}
