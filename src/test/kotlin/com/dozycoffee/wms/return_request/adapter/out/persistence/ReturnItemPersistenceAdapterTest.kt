package com.dozycoffee.wms.return_request.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.fixture.ReturnItemTestBuilder.Companion.returnItem
import com.dozycoffee.wms.return_request.fixture.ReturnRequestTestBuilder.Companion.returnRequest
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
    MockAccessScopeProvider::class,
    ProductPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ReturnRequestPersistenceAdapter::class,
    ReturnItemPersistenceAdapter::class
)
class ReturnItemPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var returnRequestPersistenceAdapter: ReturnRequestPersistenceAdapter

    @Autowired
    private lateinit var returnItemPersistenceAdapter: ReturnItemPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var returnRequestR2dbcRepository: ReturnRequestR2dbcRepository

    @Autowired
    private lateinit var returnItemR2dbcRepository: ReturnItemR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            returnItemR2dbcRepository.deleteAll()
            returnRequestR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
        }
        warehouseR2dbcRepository.deleteAll().block()
    }

    private var productSequence = 0

    private suspend fun createProductId(): Long {
        val productCode = "PRD-RETURN-${productSequence++}"
        return requireNotNull(productPersistenceAdapter.save(product().productCode(productCode).build()).productId)
    }

    private fun createWarehouseId(): Long {
        return requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
    }

    private suspend fun createReturnRequestId(warehouseId: Long): Long {
        return requireNotNull(returnRequestPersistenceAdapter.save(returnRequest().warehouseId(warehouseId).build()).returnRequestId)
    }

    @Test
    fun `반품 상품을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val returnRequestId = createReturnRequestId(warehouseId)
        val productId = createProductId()
        val newItem = returnItem().returnRequestId(returnRequestId).productId(productId).expectedQuantity(5).build()

        val saved = returnItemPersistenceAdapter.save(newItem)
        val found = returnItemPersistenceAdapter.findById(requireNotNull(saved.returnItemId))

        assertThat(found).isNotNull
        assertThat(found?.returnRequestId).isEqualTo(returnRequestId)
        assertThat(found?.productId).isEqualTo(productId)
        assertThat(found?.expectedQuantity).isEqualTo(5)
        assertThat(found?.actualQuantity).isNull()
        assertThat(found?.inspectionResult).isEqualTo(ReturnInspectionResult.PENDING)
    }

    @Test
    fun `검수 후 저장하면 실제 수량과 결과가 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val returnRequestId = createReturnRequestId(warehouseId)
        val productId = createProductId()
        val item = returnItem().returnRequestId(returnRequestId).productId(productId).expectedQuantity(5).build()
        item.inspect(actualQuantity = 4, result = ReturnInspectionResult.DEFECTIVE)

        val saved = returnItemPersistenceAdapter.save(item)
        val found = returnItemPersistenceAdapter.findById(requireNotNull(saved.returnItemId))

        assertThat(found?.actualQuantity).isEqualTo(4)
        assertThat(found?.inspectionResult).isEqualTo(ReturnInspectionResult.DEFECTIVE)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(returnItemPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `반품 ID로 목록을 조회한다`() = runTest {
        val warehouseId = createWarehouseId()
        val returnRequestId = createReturnRequestId(warehouseId)
        val otherReturnRequestId = createReturnRequestId(warehouseId)
        val target = returnItemPersistenceAdapter.save(
            returnItem().returnRequestId(returnRequestId).productId(createProductId()).build()
        )
        returnItemPersistenceAdapter.save(
            returnItem().returnRequestId(otherReturnRequestId).productId(createProductId()).build()
        )

        val result = returnItemPersistenceAdapter.findAllByReturnRequestId(returnRequestId).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().returnItemId).isEqualTo(target.returnItemId)
    }
}
