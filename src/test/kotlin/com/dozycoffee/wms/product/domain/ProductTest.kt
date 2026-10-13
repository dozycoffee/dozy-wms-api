package com.dozycoffee.wms.product.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.ProductErrorCode
import com.dozycoffee.wms.product.domain.model.Product
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ProductTest {

    private val t0: LocalDateTime = LocalDateTime.of(2026, 10, 13, 10, 0, 0)

    @Nested
    inner class 스냅샷으로_생성 {

        @Test
        fun `유효한 스냅샷이면 값을 그대로 가진다`() {
            val product = Product.of(7L, "BEAN-001", "예가체프", ProductCategory.BEAN, "KG", 180, ProductStatus.INACTIVE, t0)

            assertThat(product.productId).isEqualTo(7L)
            assertThat(product.productCode).isEqualTo("BEAN-001")
            assertThat(product.category).isEqualTo(ProductCategory.BEAN)
            assertThat(product.shelfLifeDays).isEqualTo(180)
            assertThat(product.productStatus).isEqualTo(ProductStatus.INACTIVE)
            assertThat(product.sourceOccurredAt).isEqualTo(t0)
        }

        @Test
        fun `유통기한 일수는 없어도 된다`() {
            assertThat(product().shelfLifeDays(null).build().shelfLifeDays).isNull()
        }

        @Test
        fun `상품 ID가 양수가 아니면 예외를 던진다`() {
            assertThatThrownBy { product().productId(0).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ProductErrorCode.INVALID_PRODUCT_ID.message)
        }

        @Test
        fun `상품 코드, 이름, 단위가 비어 있으면 예외를 던진다`() {
            assertThatThrownBy { product().productCode(" ").build() }.isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { product().productName(null).build() }.isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { product().unit("").build() }.isInstanceOf(InvalidDomainValueException::class.java)
        }

        @Test
        fun `유통기한 일수가 음수이면 예외를 던진다`() {
            assertThatThrownBy { product().shelfLifeDays(-1).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ProductErrorCode.INVALID_SHELF_LIFE_DAYS.message)
        }
    }

    @Nested
    inner class 이벤트_최신성 {

        @Test
        fun `반영한 이벤트가 없으면 어떤 이벤트도 오래되지 않았다`() {
            assertThat(product().sourceOccurredAt(null).build().isStale(t0)).isFalse()
        }

        @Test
        fun `반영한 이벤트보다 늦게 발생한 이벤트는 오래되지 않았다`() {
            assertThat(product().sourceOccurredAt(t0).build().isStale(t0.plusNanos(1_000))).isFalse()
        }

        @Test
        fun `같은 시각이거나 더 일찍 발생한 이벤트는 오래된 것이다`() {
            val product = product().sourceOccurredAt(t0).build()

            assertThat(product.isStale(t0)).isTrue()
            assertThat(product.isStale(t0.minusSeconds(1))).isTrue()
        }
    }

    @Test
    fun `상품 ID가 같으면 같은 상품이다`() {
        assertThat(product().productId(5L).build()).isEqualTo(product().productId(5L).productName("다른 이름").build())
    }
}
