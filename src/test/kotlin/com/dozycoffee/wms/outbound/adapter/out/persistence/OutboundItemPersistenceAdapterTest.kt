package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
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
    WarehousePersistenceAdapter::class,
    OutboundPersistenceAdapter::class,
    OutboundItemPersistenceAdapter::class
)
class OutboundItemPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var outboundPersistenceAdapter: OutboundPersistenceAdapter

    @Autowired
    private lateinit var outboundItemPersistenceAdapter: OutboundItemPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var outboundR2dbcRepository: OutboundR2dbcRepository

    @Autowired
    private lateinit var outboundItemR2dbcRepository: OutboundItemR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            outboundItemR2dbcRepository.deleteAll()
            outboundR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
        }
        warehouseR2dbcRepository.deleteAll().block()
    }

    private fun createWarehouseId(): Long {
        return requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
    }

    private suspend fun createProductId(): Long {
        return requireNotNull(productPersistenceAdapter.save(product().build()).productId)
    }

    private suspend fun createOutboundId(warehouseId: Long): Long {
        return requireNotNull(outboundPersistenceAdapter.save(outbound().warehouseId(warehouseId).build()).outboundId)
    }

    @Test
    fun `출고 상품을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val productId = createProductId()
        val outboundId = createOutboundId(warehouseId)
        val newItem = outboundItem().outboundId(outboundId).productId(productId).requestedQuantity(20).build()

        val saved = outboundItemPersistenceAdapter.save(newItem)
        val found = outboundItemPersistenceAdapter.findById(requireNotNull(saved.outboundItemId))

        assertThat(found).isNotNull
        assertThat(found?.outboundId).isEqualTo(outboundId)
        assertThat(found?.productId).isEqualTo(productId)
        assertThat(found?.requestedQuantity).isEqualTo(20)
        assertThat(found?.pickedQuantity).isNull()
    }

    @Test
    fun `피킹 결과를 저장하면 pickedQuantity가 갱신된다`() = runTest {
        val warehouseId = createWarehouseId()
        val productId = createProductId()
        val outboundId = createOutboundId(warehouseId)
        val saved = outboundItemPersistenceAdapter.save(
            outboundItem().outboundId(outboundId).productId(productId).requestedQuantity(20).build()
        )

        saved.pick(15)
        outboundItemPersistenceAdapter.save(saved)
        val found = outboundItemPersistenceAdapter.findById(requireNotNull(saved.outboundItemId))

        assertThat(found?.pickedQuantity).isEqualTo(15)
        assertThat(found?.shortageQuantity).isEqualTo(5)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(outboundItemPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `출고 ID로 목록을 조회한다`() = runTest {
        val warehouseId = createWarehouseId()
        val productId = createProductId()
        val outboundId = createOutboundId(warehouseId)
        val otherOutboundId = createOutboundId(warehouseId)
        val target = outboundItemPersistenceAdapter.save(
            outboundItem().outboundId(outboundId).productId(productId).build()
        )
        outboundItemPersistenceAdapter.save(
            outboundItem().outboundId(otherOutboundId).productId(productId).build()
        )

        val result = outboundItemPersistenceAdapter.findAllByOutboundId(outboundId).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().outboundItemId).isEqualTo(target.outboundItemId)
    }
}
