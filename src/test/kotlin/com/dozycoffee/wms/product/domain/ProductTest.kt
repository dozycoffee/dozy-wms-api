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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

class ProductTest {

    @Nested
    inner class 상품_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 상품이 활성 상태로 생성된다`() {
            val product: Product = product().build()

            assertThat(product.productCode).isEqualTo("PRD-0001")
            assertThat(product.productName).isEqualTo("콜롬비아 원두")
            assertThat(product.category).isEqualTo(ProductCategory.BEAN)
            assertThat(product.unit).isEqualTo("KG")
            assertThat(product.shelfLifeDays).isEqualTo(365)
            assertThat(product.productStatus).isEqualTo(ProductStatus.ACTIVE)
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = [" ", "   "])
        fun `상품 코드가 null이거나 공백인 경우 예외를 던진다`(invalidProductCode: String?) {
            assertThatThrownBy { product().productCode(invalidProductCode).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ProductErrorCode.INVALID_PRODUCT_CODE.message)
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = [" ", "   "])
        fun `상품명이 null이거나 공백인 경우 예외를 던진다`(invalidProductName: String?) {
            assertThatThrownBy { product().productName(invalidProductName).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ProductErrorCode.INVALID_PRODUCT_NAME.message)
        }

        @Test
        fun `카테고리가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { product().category(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ProductErrorCode.INVALID_CATEGORY.message)
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = [" ", "   "])
        fun `단위가 null이거나 공백인 경우 예외를 던진다`(invalidUnit: String?) {
            assertThatThrownBy { product().unit(invalidUnit).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ProductErrorCode.INVALID_UNIT.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, -100])
        fun `유통기한 일수가 음수인 경우 예외를 던진다`(invalidShelfLifeDays: Int) {
            assertThatThrownBy { product().shelfLifeDays(invalidShelfLifeDays).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ProductErrorCode.INVALID_SHELF_LIFE_DAYS.message)
        }

        @Test
        fun `유통기한 일수는 null을 허용한다`() {
            val product: Product = product().shelfLifeDays(null).build()

            assertThat(product.shelfLifeDays).isNull()
        }
    }

    @Nested
    inner class 상품_재구성 {

        @Test
        fun `저장된 ID와 상태로 상품 객체를 재구성한다`() {
            val product: Product = product()
                .productId(10L)
                .productStatus(ProductStatus.INACTIVE)
                .build()

            assertThat(product.productId).isEqualTo(10L)
            assertThat(product.productStatus).isEqualTo(ProductStatus.INACTIVE)
        }
    }

    @Nested
    inner class 상품_상태_전환 {

        @Test
        fun `상품을 활성화하면 상태가 ACTIVE로 변경된다`() {
            val product: Product = product().productId(1L).productStatus(ProductStatus.INACTIVE).build()

            product.activate()

            assertThat(product.productStatus).isEqualTo(ProductStatus.ACTIVE)
        }

        @Test
        fun `상품을 비활성화하면 상태가 INACTIVE로 변경된다`() {
            val product: Product = product().productId(1L).productStatus(ProductStatus.ACTIVE).build()

            product.deactivate()

            assertThat(product.productStatus).isEqualTo(ProductStatus.INACTIVE)
        }
    }

    @Nested
    inner class 상품_삭제 {

        @Test
        fun `상품을 삭제하면 isDeleted가 true가 된다`() {
            val product: Product = product().productId(1L).build()

            product.delete("system")

            assertThat(product.isDeleted()).isTrue()
            assertThat(product.deletedBy).isEqualTo("system")
        }
    }
}
