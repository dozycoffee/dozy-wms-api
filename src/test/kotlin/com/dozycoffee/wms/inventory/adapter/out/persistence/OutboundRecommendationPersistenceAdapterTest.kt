package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
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
    OutboundRecommendationPersistenceAdapter::class
)
class OutboundRecommendationPersistenceAdapterTest {

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
    private lateinit var outboundRecommendationPersistenceAdapter: OutboundRecommendationPersistenceAdapter

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

    /**
     * LotTestBuilder.build()는 lotId가 없으면 Lot.create()를 호출하는데, 이 경로는 항상
     * lotStatus=NORMAL로 생성되고 builder의 lotStatus 설정을 무시한다 — EXPIRING_SOON/EXPIRED로
     * 만들려면 저장 후 도메인 전이 메서드를 호출해 다시 저장해야 한다.
     */
    private suspend fun createLot(productId: Long, lotNumber: String, targetStatus: LotStatus): Long {
        val saved = lotPersistenceAdapter.save(lot().productId(productId).lotNumber(lotNumber).build())
        when (targetStatus) {
            LotStatus.EXPIRING_SOON -> saved.markExpiringSoon()
            LotStatus.EXPIRED -> saved.markExpired()
            LotStatus.NORMAL -> {}
        }
        if (targetStatus != LotStatus.NORMAL) {
            lotPersistenceAdapter.save(saved)
        }
        return requireNotNull(saved.lotId)
    }

    private fun createLocation(): Long {
        val warehouseId: Long = requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
        val zoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).zoneCode(ZoneCode.A).build())
                .map { requireNotNull(it.zoneId) }.block()
        )
        return requireNotNull(
            locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode("A-01").build())
                .map { requireNotNull(it.locationId) }.block()
        )
    }

    @Test
    fun `유통기한 임박이면서 가용 재고가 남은 Lot을 우선 출고 권고 대상으로 조회한다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(
            productPersistenceAdapter.save(product().productName("콜롬비아 원두").build()).productId
        )
        val lotId = createLot(productId, "LOT-20260101-001", LotStatus.EXPIRING_SOON)
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).quantity(15).build()
        )

        val result = outboundRecommendationPersistenceAdapter.findAll().toList()

        assertThat(result).hasSize(1)
        assertThat(result[0].lotId).isEqualTo(lotId)
        assertThat(result[0].productId).isEqualTo(productId)
        assertThat(result[0].productName).isEqualTo("콜롬비아 원두")
        assertThat(result[0].availableQuantity).isEqualTo(15)
    }

    @Test
    fun `유통기한 임박(EXPIRING_SOON)이 아닌 Lot은 제외한다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val normalLotId = createLot(productId, "LOT-20260101-001", LotStatus.NORMAL)
        val expiredLotId = createLot(productId, "LOT-20260101-002", LotStatus.EXPIRED)
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(normalLotId).locationId(locationId).quantity(10).build()
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(expiredLotId).locationId(locationId).quantity(10).build()
        )

        val result = outboundRecommendationPersistenceAdapter.findAll().toList()

        assertThat(result).isEmpty()
    }

    @Test
    fun `전량 점유되어 가용 재고가 없는 Lot은 제외한다`() = runTest {
        val locationId = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = createLot(productId, "LOT-20260101-001", LotStatus.EXPIRING_SOON)
        val fullyHeld = inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId).quantity(10).build()
        )
        fullyHeld.hold(10)
        inventoryPersistenceAdapter.save(fullyHeld)

        val result = outboundRecommendationPersistenceAdapter.findAll().toList()

        assertThat(result).isEmpty()
    }

    @Test
    fun `여러 Location에 걸친 Lot의 가용 재고는 합산된다`() = runTest {
        val locationId1 = createLocation()
        val locationId2 = createLocation()
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val lotId = createLot(productId, "LOT-20260101-001", LotStatus.EXPIRING_SOON)
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId1).quantity(10).build()
        )
        inventoryPersistenceAdapter.save(
            inventory().productId(productId).lotId(lotId).locationId(locationId2).quantity(5).build()
        )

        val result = outboundRecommendationPersistenceAdapter.findAll().toList()

        assertThat(result).hasSize(1)
        assertThat(result[0].availableQuantity).isEqualTo(15)
    }
}
