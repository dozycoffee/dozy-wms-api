package com.dozycoffee.wms.outbound.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.outbound.domain.exception.InvalidPickedQuantityException
import com.dozycoffee.wms.outbound.domain.exception.OutboundItemAlreadyPickedException
import com.dozycoffee.wms.outbound.domain.exception.OutboundItemErrorCode
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OutboundItemTest {

    @Nested
    inner class 출고_상품_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 피킹 전 상태로 생성된다`() {
            val item: OutboundItem = outboundItem().build()

            assertThat(item.outboundId).isEqualTo(1L)
            assertThat(item.productId).isEqualTo(1L)
            assertThat(item.requestedQuantity).isEqualTo(10)
            assertThat(item.pickedQuantity).isNull()
            assertThat(item.shortageQuantity).isNull()
        }

        @Test
        fun `출고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { outboundItem().outboundId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(OutboundItemErrorCode.INVALID_OUTBOUND_ID.message)
        }

        @Test
        fun `상품 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { outboundItem().productId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(OutboundItemErrorCode.INVALID_PRODUCT_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `출고 요청 수량이 0 이하이면 예외를 던진다`(invalidQuantity: Int) {
            assertThatThrownBy { outboundItem().requestedQuantity(invalidQuantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(OutboundItemErrorCode.INVALID_REQUESTED_QUANTITY.message)
        }
    }

    @Nested
    inner class 출고_상품_재구성 {

        @Test
        fun `저장된 ID와 피킹 수량으로 출고 상품을 재구성한다`() {
            val item: OutboundItem = outboundItem().outboundItemId(100L).pickedQuantity(8).build()

            assertThat(item.outboundItemId).isEqualTo(100L)
            assertThat(item.pickedQuantity).isEqualTo(8)
        }
    }

    @Nested
    inner class 피킹 {

        @Test
        fun `요청 수량만큼 피킹하면 부족분이 0이다`() {
            val item: OutboundItem = outboundItem().outboundItemId(1L).requestedQuantity(10).build()

            item.pick(10)

            assertThat(item.pickedQuantity).isEqualTo(10)
            assertThat(item.shortageQuantity).isZero()
        }

        @Test
        fun `재고 부족으로 요청 수량보다 적게 피킹하면 부족분이 기록된다`() {
            val item: OutboundItem = outboundItem().outboundItemId(1L).requestedQuantity(10).build()

            item.pick(7)

            assertThat(item.shortageQuantity).isEqualTo(3)
        }

        @Test
        fun `이미 피킹된 상품을 다시 피킹하면 예외를 던진다`() {
            val item: OutboundItem = outboundItem().outboundItemId(1L).pickedQuantity(10).build()

            assertThatThrownBy { item.pick(10) }
                .isInstanceOf(OutboundItemAlreadyPickedException::class.java)
                .hasMessage(OutboundItemErrorCode.ALREADY_PICKED.message)
        }

        @Test
        fun `피킹 수량이 음수이면 예외를 던진다`() {
            val item: OutboundItem = outboundItem().outboundItemId(1L).requestedQuantity(10).build()

            assertThatThrownBy { item.pick(-1) }
                .isInstanceOf(InvalidPickedQuantityException::class.java)
                .hasMessage(OutboundItemErrorCode.INVALID_PICKED_QUANTITY.message)
        }

        @Test
        fun `피킹 수량이 요청 수량을 초과하면 예외를 던진다`() {
            val item: OutboundItem = outboundItem().outboundItemId(1L).requestedQuantity(10).build()

            assertThatThrownBy { item.pick(11) }
                .isInstanceOf(InvalidPickedQuantityException::class.java)
                .hasMessage(OutboundItemErrorCode.INVALID_PICKED_QUANTITY.message)
        }
    }
}
