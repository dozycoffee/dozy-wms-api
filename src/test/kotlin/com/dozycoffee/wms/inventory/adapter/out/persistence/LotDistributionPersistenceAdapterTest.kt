package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
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
    MockAccessScopeProvider::class,
    ProductPersistenceAdapter::class,
    LotPersistenceAdapter::class,
    InventoryPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    LocationPersistenceAdapter::class,
    LotDistributionPersistenceAdapter::class
)
class LotDistributionPersistenceAdapterTest {

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
    private lateinit var lotDistributionPersistenceAdapter: LotDistributionPersistenceAdapter

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

    private fun createLocation(zoneCode: ZoneCode, locationCode: String): Long {
        val warehouseId: Long = requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
        val zoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).zoneCode(zoneCode).build())
                .map { requireNotNull(it.zoneId) }.block()
        )
        return requireNotNull(
            locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode(locationCode).build())
                .map { requireNotNull(it.locationId) }.block()
        )
    }

    @Test
    fun `Lot의 Zone Location별 재고 분포를 집계한다`() = runTest {
        val locationA = createLocation(ZoneCode.A, "A-01")
        val locationB = createLocation(ZoneCode.B, "B-01")
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val targetLotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)
        val otherLotId = requireNotNull(
            lotPersistenceAdapter.save(lot().productId(productId).lotNumber("LOT-OTHER").build()).lotId
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(targetLotId).locationId(locationA).quantity(20).build()
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(targetLotId).locationId(locationB).quantity(15).build()
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(otherLotId).locationId(locationA).quantity(99).build()
        )

        val result = lotDistributionPersistenceAdapter.findAllByLotId(targetLotId).toList()

        assertThat(result).hasSize(2)
        val byLocation = result.associateBy { it.locationId }
        assertThat(byLocation[locationA]?.zoneCode).isEqualTo(ZoneCode.A)
        assertThat(byLocation[locationA]?.quantity).isEqualTo(20)
        assertThat(byLocation[locationB]?.zoneCode).isEqualTo(ZoneCode.B)
        assertThat(byLocation[locationB]?.quantity).isEqualTo(15)
    }

    @Test
    fun `재고가 없는 Lot은 빈 분포를 반환한다`() = runTest {
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = requireNotNull(lotPersistenceAdapter.save(lot().productId(productId).build()).lotId)

        val result = lotDistributionPersistenceAdapter.findAllByLotId(lotId).toList()

        assertThat(result).isEmpty()
    }
}
