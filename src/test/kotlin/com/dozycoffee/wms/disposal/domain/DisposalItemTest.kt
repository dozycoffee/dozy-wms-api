package com.dozycoffee.wms.disposal.domain

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.exception.DisposalItemErrorCode
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalItemTestBuilder.Companion.disposalItem
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DisposalItemTest {

    @Nested
    inner class 폐기_상품_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 생성된다`() {
            val item: DisposalItem = disposalItem().build()

            assertThat(item.disposalId).isEqualTo(1L)
            assertThat(item.inventoryId).isEqualTo(1L)
            assertThat(item.quantity).isEqualTo(10)
            assertThat(item.reason).isEqualTo(DisposalReason.EXPIRED)
        }

        @Test
        fun `폐기 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { disposalItem().disposalId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(DisposalItemErrorCode.INVALID_DISPOSAL_ID.message)
        }

        @Test
        fun `재고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { disposalItem().inventoryId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(DisposalItemErrorCode.INVALID_INVENTORY_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `폐기 수량이 0 이하이면 예외를 던진다`(invalidQuantity: Int) {
            assertThatThrownBy { disposalItem().quantity(invalidQuantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(DisposalItemErrorCode.INVALID_QUANTITY.message)
        }
    }

    @Nested
    inner class 폐기_상품_재구성 {

        @Test
        fun `저장된 ID로 폐기 상품을 재구성한다`() {
            val item: DisposalItem = disposalItem().disposalItemId(100L).reason(DisposalReason.RETURN_DEFECT).build()

            assertThat(item.disposalItemId).isEqualTo(100L)
            assertThat(item.reason).isEqualTo(DisposalReason.RETURN_DEFECT)
        }
    }
}
