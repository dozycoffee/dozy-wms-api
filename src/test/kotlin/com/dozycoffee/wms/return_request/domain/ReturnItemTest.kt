package com.dozycoffee.wms.return_request.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.exception.InvalidActualQuantityException
import com.dozycoffee.wms.return_request.domain.exception.InvalidInspectionResultException
import com.dozycoffee.wms.return_request.domain.exception.ReturnItemAlreadyInspectedException
import com.dozycoffee.wms.return_request.domain.exception.ReturnItemErrorCode
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import com.dozycoffee.wms.return_request.fixture.ReturnItemTestBuilder.Companion.returnItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ReturnItemTest {

    @Nested
    inner class 반품_상품_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 PENDING 상태로 생성된다`() {
            val item: ReturnItem = returnItem().build()

            assertThat(item.returnRequestId).isEqualTo(1L)
            assertThat(item.productId).isEqualTo(1L)
            assertThat(item.expectedQuantity).isEqualTo(10)
            assertThat(item.actualQuantity).isNull()
            assertThat(item.inspectionResult).isEqualTo(ReturnInspectionResult.PENDING)
            assertThat(item.quantityDiscrepancy).isNull()
        }

        @Test
        fun `반품 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { returnItem().returnRequestId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ReturnItemErrorCode.INVALID_RETURN_REQUEST_ID.message)
        }

        @Test
        fun `상품 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { returnItem().productId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ReturnItemErrorCode.INVALID_PRODUCT_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `반품 예정 수량이 0 이하이면 예외를 던진다`(invalidQuantity: Int) {
            assertThatThrownBy { returnItem().expectedQuantity(invalidQuantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ReturnItemErrorCode.INVALID_EXPECTED_QUANTITY.message)
        }
    }

    @Nested
    inner class 반품_상품_재구성 {

        @Test
        fun `저장된 ID와 검수 결과로 반품 상품을 재구성한다`() {
            val item: ReturnItem = returnItem()
                .returnItemId(100L)
                .actualQuantity(8)
                .inspectionResult(ReturnInspectionResult.NORMAL)
                .build()

            assertThat(item.returnItemId).isEqualTo(100L)
            assertThat(item.actualQuantity).isEqualTo(8)
            assertThat(item.inspectionResult).isEqualTo(ReturnInspectionResult.NORMAL)
        }
    }

    @Nested
    inner class 검수 {

        @Test
        fun `정상 판정 시 실제 수량과 결과가 기록된다`() {
            val item: ReturnItem = returnItem().returnItemId(1L).expectedQuantity(10).build()

            item.inspect(actualQuantity = 10, result = ReturnInspectionResult.NORMAL)

            assertThat(item.actualQuantity).isEqualTo(10)
            assertThat(item.inspectionResult).isEqualTo(ReturnInspectionResult.NORMAL)
            assertThat(item.quantityDiscrepancy).isZero()
        }

        @Test
        fun `실제 수량이 예정 수량과 다르면 차이가 기록된다`() {
            val item: ReturnItem = returnItem().returnItemId(1L).expectedQuantity(10).build()

            item.inspect(actualQuantity = 7, result = ReturnInspectionResult.DEFECTIVE)

            assertThat(item.quantityDiscrepancy).isEqualTo(-3)
        }

        @Test
        fun `이미 검수된 상품을 다시 검수하면 예외를 던진다`() {
            val item: ReturnItem = returnItem()
                .returnItemId(1L)
                .actualQuantity(10)
                .inspectionResult(ReturnInspectionResult.NORMAL)
                .build()

            assertThatThrownBy { item.inspect(actualQuantity = 10, result = ReturnInspectionResult.NORMAL) }
                .isInstanceOf(ReturnItemAlreadyInspectedException::class.java)
                .hasMessage(ReturnItemErrorCode.ALREADY_INSPECTED.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, -10])
        fun `실제 수량이 음수이면 예외를 던진다`(invalidQuantity: Int) {
            val item: ReturnItem = returnItem().returnItemId(1L).build()

            assertThatThrownBy {
                item.inspect(actualQuantity = invalidQuantity, result = ReturnInspectionResult.NORMAL)
            }
                .isInstanceOf(InvalidActualQuantityException::class.java)
                .hasMessage(ReturnItemErrorCode.INVALID_ACTUAL_QUANTITY.message)
        }

        @Test
        fun `검수 결과로 PENDING을 전달하면 예외를 던진다`() {
            val item: ReturnItem = returnItem().returnItemId(1L).build()

            assertThatThrownBy { item.inspect(actualQuantity = 10, result = ReturnInspectionResult.PENDING) }
                .isInstanceOf(InvalidInspectionResultException::class.java)
                .hasMessage(ReturnItemErrorCode.INVALID_INSPECTION_RESULT.message)
        }
    }
}
