package com.dozycoffee.wms.product.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.support.SystemActorProvider
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
@Import(R2dbcConfig::class, SystemActorProvider::class, ProductPersistenceAdapter::class)
class ProductPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest { productR2dbcRepository.deleteAll() }
    }

    @Test
    fun `이벤트가 정한 상품 ID 그대로 새로 저장하고 정보가 왕복된다`() = runTest {
        val occurredAt = LocalDateTime.of(2026, 10, 13, 10, 0, 0)
        val product = product().productId(777L).productCode("BEAN-777").productName("예가체프")
            .category(ProductCategory.BEAN).unit("KG").shelfLifeDays(180).sourceOccurredAt(occurredAt).build()

        val saved = productPersistenceAdapter.save(product)

        assertThat(saved.productId).isEqualTo(777L)
        val found = productPersistenceAdapter.findById(777L)
        assertThat(found?.productCode).isEqualTo("BEAN-777")
        assertThat(found?.productName).isEqualTo("예가체프")
        assertThat(found?.category).isEqualTo(ProductCategory.BEAN)
        assertThat(found?.shelfLifeDays).isEqualTo(180)
        assertThat(found?.productStatus).isEqualTo(ProductStatus.ACTIVE)
        assertThat(found?.sourceOccurredAt).isEqualTo(occurredAt)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(productPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `같은 상품 ID로 다시 저장하면 갱신하고 생성 시각은 유지한다`() = runTest {
        productPersistenceAdapter.save(product().productId(778L).productCode("BEAN-778").build())
        val createdAt = productR2dbcRepository.findById(778L)?.createdAt

        productPersistenceAdapter.save(
            product().productId(778L).productCode("BEAN-778").productName("바뀐 이름").productStatus(ProductStatus.INACTIVE).build()
        )

        val found = productPersistenceAdapter.findById(778L)
        assertThat(found?.productName).isEqualTo("바뀐 이름")
        assertThat(found?.productStatus).isEqualTo(ProductStatus.INACTIVE)
        assertThat(productR2dbcRepository.findById(778L)?.createdAt).isEqualTo(createdAt)
        assertThat(productR2dbcRepository.count()).isEqualTo(1)
    }

    @Test
    fun `카테고리와 상태로 목록을 필터링하고 상품 ID 순으로 정렬한다`() = runTest {
        productPersistenceAdapter.save(product().productId(782L).productCode("SYR-1").category(ProductCategory.SYRUP).build())
        productPersistenceAdapter.save(product().productId(781L).productCode("BEAN-2").category(ProductCategory.BEAN).build())
        productPersistenceAdapter.save(
            product().productId(780L).productCode("BEAN-1").category(ProductCategory.BEAN).productStatus(ProductStatus.INACTIVE).build()
        )

        assertThat(productPersistenceAdapter.findAll(null, null).toList().map { it.productId })
            .containsExactly(780L, 781L, 782L)
        assertThat(productPersistenceAdapter.findAll(ProductCategory.BEAN, null).toList().map { it.productId })
            .containsExactly(780L, 781L)
        assertThat(productPersistenceAdapter.findAll(ProductCategory.BEAN, ProductStatus.ACTIVE).toList().map { it.productId })
            .containsExactly(781L)
    }
}
