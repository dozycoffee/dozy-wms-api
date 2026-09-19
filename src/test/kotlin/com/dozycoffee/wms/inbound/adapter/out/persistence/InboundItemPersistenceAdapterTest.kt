package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
import com.dozycoffee.wms.inbound.fixture.InboundTestBuilder.Companion.inbound
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
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
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    InboundPersistenceAdapter::class,
    InboundItemPersistenceAdapter::class
)
class InboundItemPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var inboundPersistenceAdapter: InboundPersistenceAdapter

    @Autowired
    private lateinit var inboundItemPersistenceAdapter: InboundItemPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var inboundR2dbcRepository: InboundR2dbcRepository

    @Autowired
    private lateinit var inboundItemR2dbcRepository: InboundItemR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            inboundItemR2dbcRepository.deleteAll()
            inboundR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
        }
        zoneR2dbcRepository.deleteAll().block()
        warehouseR2dbcRepository.deleteAll().block()
    }

    private fun createWarehouseId(): Long {
        return requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
    }

    private fun createZoneId(warehouseId: Long): Long {
        return requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).map { requireNotNull(it.zoneId) }.block()
        )
    }

    private suspend fun createProductId(): Long {
        return requireNotNull(productPersistenceAdapter.save(product().build()).productId)
    }

    private suspend fun createInboundId(warehouseId: Long): Long {
        return requireNotNull(inboundPersistenceAdapter.save(inbound().warehouseId(warehouseId).build()).inboundId)
    }

    @Test
    fun `입고 상품을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val zoneId = createZoneId(warehouseId)
        val productId = createProductId()
        val inboundId = createInboundId(warehouseId)
        val newItem = inboundItem()
            .inboundId(inboundId).productId(productId).zoneId(zoneId).expectedQuantity(20)
            .build()

        val saved = inboundItemPersistenceAdapter.save(newItem)
        val found = inboundItemPersistenceAdapter.findById(requireNotNull(saved.inboundItemId))

        assertThat(found).isNotNull
        assertThat(found?.inboundId).isEqualTo(inboundId)
        assertThat(found?.productId).isEqualTo(productId)
        assertThat(found?.zoneId).isEqualTo(zoneId)
        assertThat(found?.expectedQuantity).isEqualTo(20)
        assertThat(found?.inspectionResult).isEqualTo(InspectionResult.PENDING)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(inboundItemPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `입고 ID로 목록을 조회한다`() = runTest {
        val warehouseId = createWarehouseId()
        val zoneId = createZoneId(warehouseId)
        val productId = createProductId()
        val inboundId = createInboundId(warehouseId)
        val otherInboundId = createInboundId(warehouseId)
        val target = inboundItemPersistenceAdapter.save(
            inboundItem().inboundId(inboundId).productId(productId).zoneId(zoneId).build()
        )
        inboundItemPersistenceAdapter.save(
            inboundItem().inboundId(otherInboundId).productId(productId).zoneId(zoneId).build()
        )

        val result = inboundItemPersistenceAdapter.findAllByInboundId(inboundId).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().inboundItemId).isEqualTo(target.inboundItemId)
    }
}
