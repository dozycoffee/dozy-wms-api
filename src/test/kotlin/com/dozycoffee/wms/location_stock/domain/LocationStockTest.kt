package com.dozycoffee.wms.location_stock.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.location_stock.domain.exception.InsufficientHeldLocationStockQuantityException
import com.dozycoffee.wms.location_stock.domain.exception.InsufficientLocationStockQuantityException
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockErrorCode
import com.dozycoffee.wms.location_stock.fixture.LocationStockTestBuilder.Companion.locationStock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LocationStockTest {

    @Nested
    inner class 생성 {

        @Test
        fun `유효한 값으로 생성한다`() {
            val stock = locationStock().locationId(3L).productId(7L).lotNumber("L-1").quantity(5).build()

            assertThat(stock.locationId).isEqualTo(3L)
            assertThat(stock.productId).isEqualTo(7L)
            assertThat(stock.lotNumber).isEqualTo("L-1")
            assertThat(stock.quantity).isEqualTo(5)
            assertThat(stock.locationStockId).isNull()
        }

        @Test
        fun `위치가 없으면 예외를 던진다`() {
            assertThatThrownBy { locationStock().locationId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(LocationStockErrorCode.INVALID_LOCATION_ID.message)
        }

        @Test
        fun `상품이 없으면 예외를 던진다`() {
            assertThatThrownBy { locationStock().productId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(LocationStockErrorCode.INVALID_PRODUCT_ID.message)
        }

        @Test
        fun `Lot 번호가 비어 있거나 50자를 넘으면 예외를 던진다`() {
            assertThatThrownBy { locationStock().lotNumber(" ").build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { locationStock().lotNumber(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { locationStock().lotNumber("L".repeat(51)).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
        }

        @Test
        fun `수량이 0 이하이면 예외를 던진다`() {
            assertThatThrownBy { locationStock().quantity(0).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(LocationStockErrorCode.INVALID_AMOUNT.message)
        }
    }

    @Nested
    inner class 수량_변경 {

        @Test
        fun `증가하면 수량이 늘어난다`() {
            val stock = locationStock().quantity(10).build()

            stock.increase(5)

            assertThat(stock.quantity).isEqualTo(15)
        }

        @Test
        fun `감소하면 수량이 줄고 0까지 줄일 수 있다`() {
            val stock = locationStock().quantity(10).build()

            stock.decrease(10)

            assertThat(stock.quantity).isEqualTo(0)
        }

        @Test
        fun `보유 수량보다 많이 감소하면 예외를 던진다`() {
            val stock = locationStock().quantity(10).build()

            assertThatThrownBy { stock.decrease(11) }
                .isInstanceOf(InsufficientLocationStockQuantityException::class.java)
            assertThat(stock.quantity).isEqualTo(10)
        }

        @Test
        fun `증감 수량이 0 이하이면 예외를 던진다`() {
            val stock = locationStock().build()

            assertThatThrownBy { stock.increase(0) }.isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { stock.decrease(-1) }.isInstanceOf(InvalidDomainValueException::class.java)
        }
    }

    @Nested
    inner class 동등성 {

        @Test
        fun `식별자가 같으면 같은 장부 행이다`() {
            val a = locationStock().locationStockId(1L).build()
            val b = locationStock().locationStockId(1L).quantity(99).build()

            assertThat(a).isEqualTo(b)
            assertThat(a.hashCode()).isEqualTo(b.hashCode())
        }

        @Test
        fun `식별자가 없으면 서로 다른 인스턴스는 같지 않다`() {
            assertThat(locationStock().build()).isNotEqualTo(locationStock().build())
        }
    }

    @Nested
    inner class 점유 {

        @Test
        fun `가용 수량은 총 수량에서 점유 수량을 뺀 값이다`() {
            val stock = locationStock().locationStockId(1L).quantity(10).heldQuantity(4).build()

            assertThat(stock.availableQuantity).isEqualTo(6)
        }

        @Test
        fun `가용 수량 안에서 점유하고 해제한다`() {
            val stock = locationStock().locationStockId(1L).quantity(10).build()

            stock.hold(7)
            assertThat(stock.heldQuantity).isEqualTo(7)
            assertThat(stock.quantity).isEqualTo(10)

            stock.releaseHold(3)
            assertThat(stock.heldQuantity).isEqualTo(4)
        }

        @Test
        fun `가용 수량보다 많이 점유하면 예외를 던진다`() {
            val stock = locationStock().locationStockId(1L).quantity(10).heldQuantity(4).build()

            assertThatThrownBy { stock.hold(7) }.isInstanceOf(InsufficientLocationStockQuantityException::class.java)
        }

        @Test
        fun `점유 수량보다 많이 해제하면 예외를 던진다`() {
            val stock = locationStock().locationStockId(1L).quantity(10).heldQuantity(2).build()

            assertThatThrownBy { stock.releaseHold(3) }.isInstanceOf(InsufficientHeldLocationStockQuantityException::class.java)
        }

        @Test
        fun `소비하면 총 수량과 점유 수량이 함께 준다`() {
            val stock = locationStock().locationStockId(1L).quantity(10).heldQuantity(6).build()

            stock.consume(4)

            assertThat(stock.quantity).isEqualTo(6)
            assertThat(stock.heldQuantity).isEqualTo(2)
        }

        @Test
        fun `점유 수량보다 많이 소비하면 예외를 던진다`() {
            val stock = locationStock().locationStockId(1L).quantity(10).heldQuantity(2).build()

            assertThatThrownBy { stock.consume(3) }.isInstanceOf(InsufficientHeldLocationStockQuantityException::class.java)
        }

        @Test
        fun `점유된 수량은 감소할 수 없다`() {
            val stock = locationStock().locationStockId(1L).quantity(10).heldQuantity(8).build()

            assertThatThrownBy { stock.decrease(3) }.isInstanceOf(InsufficientLocationStockQuantityException::class.java)
            stock.decrease(2)
            assertThat(stock.quantity).isEqualTo(8)
        }
    }
}
