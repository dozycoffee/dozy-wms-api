package com.dozycoffee.wms.product.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import

@DataR2dbcTest
@Import(R2dbcConfig::class, MockAccessScopeProvider::class, ProductPersistenceAdapter::class)
class ProductPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @AfterEach
    fun cleanUp() = runTest {
        productR2dbcRepository.deleteAll()
    }

    @Test
    fun `상품을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val product = product().build()

        val saved = productPersistenceAdapter.save(product)
        val found = productPersistenceAdapter.findById(requireNotNull(saved.productId))

        assertThat(found).isNotNull
        assertThat(found?.productCode).isEqualTo(product.productCode)
        assertThat(found?.productName).isEqualTo(product.productName)
        assertThat(found?.category).isEqualTo(product.category)
        assertThat(found?.productStatus).isEqualTo(ProductStatus.ACTIVE)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        val found = productPersistenceAdapter.findById(999_999L)

        assertThat(found).isNull()
    }

    @Test
    fun `이미 저장된 상품을 다시 저장해도 생성 시각이 유지된다`() = runTest {
        val saved = productPersistenceAdapter.save(product().build())
        val productId = requireNotNull(saved.productId)
        val beforeUpdate = requireNotNull(productR2dbcRepository.findById(productId))
        saved.deactivate()

        productPersistenceAdapter.save(saved)
        val afterUpdate = requireNotNull(productR2dbcRepository.findById(productId))

        assertThat(afterUpdate.productStatus)
            .isEqualTo(CommonCodes.toCode("PRODUCT_STATUS", ProductStatus.INACTIVE))
        assertThat(afterUpdate.createdAt).isEqualTo(beforeUpdate.createdAt)
        assertThat(afterUpdate.createdBy).isEqualTo(beforeUpdate.createdBy)
    }

    @Test
    fun `등록된 상품 코드는 존재하는 것으로 판단한다`() = runTest {
        productPersistenceAdapter.save(product().productCode("PRD-EXISTS").build())

        assertThat(productPersistenceAdapter.existsByProductCode("PRD-EXISTS")).isTrue()
        assertThat(productPersistenceAdapter.existsByProductCode("PRD-NOT-EXISTS")).isFalse()
    }

    @Test
    fun `상품을 삭제하면 삭제 상태가 영속화되고 findById로 조회되지 않는다`() = runTest {
        val saved = productPersistenceAdapter.save(product().build())
        val productId = requireNotNull(saved.productId)

        saved.delete("system")
        productPersistenceAdapter.save(saved)

        val found = productPersistenceAdapter.findById(productId)
        val persisted = requireNotNull(productR2dbcRepository.findById(productId))

        assertThat(found).isNull()
        assertThat(persisted.isDeleted()).isTrue()
        assertThat(persisted.deletedBy).isEqualTo("system")
    }

    @Test
    fun `카테고리와 상태로 목록을 필터링하고 삭제된 상품은 제외한다`() = runTest {
        val bean = productPersistenceAdapter.save(
            product().productCode("PRD-BEAN").category(ProductCategory.BEAN).build()
        )
        productPersistenceAdapter.save(
            product().productCode("PRD-SYRUP").category(ProductCategory.SYRUP).build()
        )
        val deletedBean = product().productCode("PRD-BEAN-DELETED").category(ProductCategory.BEAN).build()
        deletedBean.delete("system")
        productPersistenceAdapter.save(deletedBean)

        val result = productPersistenceAdapter.findAll(ProductCategory.BEAN, null).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().productId).isEqualTo(bean.productId)
    }
}
