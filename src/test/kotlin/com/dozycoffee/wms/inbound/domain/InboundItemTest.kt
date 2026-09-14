package com.dozycoffee.wms.inbound.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.InboundItemAlreadyInspectedException
import com.dozycoffee.wms.inbound.domain.exception.InboundItemErrorCode
import com.dozycoffee.wms.inbound.domain.exception.InvalidActualQuantityException
import com.dozycoffee.wms.inbound.domain.exception.InvalidInspectionResultException
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class InboundItemTest {

    @Nested
    inner class 입고_상품_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 PENDING 상태로 생성된다`() {
            val item: InboundItem = inboundItem().build()

            assertThat(item.inboundId).isEqualTo(1L)
            assertThat(item.productId).isEqualTo(1L)
            assertThat(item.zoneId).isEqualTo(1L)
            assertThat(item.expectedQuantity).isEqualTo(10)
            assertThat(item.actualQuantity).isNull()
            assertThat(item.inspectionResult).isEqualTo(InspectionResult.PENDING)
            assertThat(item.quantityDiscrepancy).isNull()
        }

        @Test
        fun `입고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundItem().inboundId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_INBOUND_ID.message)
        }

        @Test
        fun `상품 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundItem().productId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_PRODUCT_ID.message)
        }

        @Test
        fun `Zone ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundItem().zoneId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_ZONE_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `입고 예정 수량이 0 이하이면 예외를 던진다`(invalidQuantity: Int) {
            assertThatThrownBy { inboundItem().expectedQuantity(invalidQuantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_EXPECTED_QUANTITY.message)
        }
    }

    @Nested
    inner class 입고_상품_재구성 {

        @Test
        fun `저장된 ID와 검수 결과로 입고 상품을 재구성한다`() {
            val item: InboundItem = inboundItem()
                .inboundItemId(100L)
                .actualQuantity(8)
                .inspectionResult(InspectionResult.NORMAL)
                .build()

            assertThat(item.inboundItemId).isEqualTo(100L)
            assertThat(item.actualQuantity).isEqualTo(8)
            assertThat(item.inspectionResult).isEqualTo(InspectionResult.NORMAL)
        }
    }

    @Nested
    inner class 검수 {

        @Test
        fun `정상 판정 시 실제 수량과 결과가 기록된다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            item.inspect(actualQuantity = 10, result = InspectionResult.NORMAL)

            assertThat(item.actualQuantity).isEqualTo(10)
            assertThat(item.inspectionResult).isEqualTo(InspectionResult.NORMAL)
            assertThat(item.quantityDiscrepancy).isZero()
        }

        @Test
        fun `실제 수량이 예정 수량과 다르면 차이가 기록된다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            item.inspect(actualQuantity = 7, result = InspectionResult.DEFECTIVE)

            assertThat(item.quantityDiscrepancy).isEqualTo(-3)
        }

        @Test
        fun `이미 검수된 상품을 다시 검수하면 예외를 던진다`() {
            val item: InboundItem = inboundItem()
                .inboundItemId(1L)
                .actualQuantity(10)
                .inspectionResult(InspectionResult.NORMAL)
                .build()

            assertThatThrownBy { item.inspect(actualQuantity = 10, result = InspectionResult.NORMAL) }
                .isInstanceOf(InboundItemAlreadyInspectedException::class.java)
                .hasMessage(InboundItemErrorCode.ALREADY_INSPECTED.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, -10])
        fun `실제 수량이 음수이면 예외를 던진다`(invalidQuantity: Int) {
            val item: InboundItem = inboundItem().inboundItemId(1L).build()

            assertThatThrownBy { item.inspect(actualQuantity = invalidQuantity, result = InspectionResult.NORMAL) }
                .isInstanceOf(InvalidActualQuantityException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_ACTUAL_QUANTITY.message)
        }

        @Test
        fun `검수 결과로 PENDING을 전달하면 예외를 던진다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).build()

            assertThatThrownBy { item.inspect(actualQuantity = 10, result = InspectionResult.PENDING) }
                .isInstanceOf(InvalidInspectionResultException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_INSPECTION_RESULT.message)
        }
    }
}
