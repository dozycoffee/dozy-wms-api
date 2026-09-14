package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.exception.AllocationErrorCode
import com.dozycoffee.wms.inventory.domain.exception.InvalidAllocationStatusTransitionException
import com.dozycoffee.wms.inventory.domain.model.Allocation
import com.dozycoffee.wms.inventory.fixture.AllocationTestBuilder.Companion.allocation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class AllocationTest {

    @Nested
    inner class 점유_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 HELD 상태로 생성된다`() {
            val allocation: Allocation = allocation().build()

            assertThat(allocation.inventoryId).isEqualTo(1L)
            assertThat(allocation.referenceType).isEqualTo(AllocationReferenceType.OUTBOUND)
            assertThat(allocation.referenceId).isEqualTo(1L)
            assertThat(allocation.quantity).isEqualTo(10)
            assertThat(allocation.status).isEqualTo(AllocationStatus.HELD)
        }

        @Test
        fun `재고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { allocation().inventoryId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(AllocationErrorCode.INVALID_INVENTORY_ID.message)
        }

        @Test
        fun `참조 유형이 null이면 예외를 던진다`() {
            assertThatThrownBy { allocation().referenceType(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(AllocationErrorCode.INVALID_REFERENCE_TYPE.message)
        }

        @Test
        fun `참조 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { allocation().referenceId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(AllocationErrorCode.INVALID_REFERENCE_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `점유 수량이 0 이하이면 예외를 던진다`(invalidQuantity: Int) {
            assertThatThrownBy { allocation().quantity(invalidQuantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(AllocationErrorCode.INVALID_QUANTITY.message)
        }
    }

    @Nested
    inner class 점유_재구성 {

        @Test
        fun `저장된 ID와 상태로 점유 객체를 재구성한다`() {
            val allocation: Allocation = allocation().allocationId(100L).status(AllocationStatus.RELEASED).build()

            assertThat(allocation.allocationId).isEqualTo(100L)
            assertThat(allocation.status).isEqualTo(AllocationStatus.RELEASED)
        }
    }

    @Nested
    inner class 점유_해제 {

        @Test
        fun `HELD 상태의 점유는 RELEASED로 전환된다`() {
            val allocation: Allocation = allocation().allocationId(1L).status(AllocationStatus.HELD).build()

            allocation.release()

            assertThat(allocation.status).isEqualTo(AllocationStatus.RELEASED)
        }

        @ParameterizedTest
        @EnumSource(value = AllocationStatus::class, names = ["RELEASED", "FULFILLED"])
        fun `이미 종결된 점유는 해제할 수 없다`(finalStatus: AllocationStatus) {
            val allocation: Allocation = allocation().allocationId(1L).status(finalStatus).build()

            assertThatThrownBy { allocation.release() }
                .isInstanceOf(InvalidAllocationStatusTransitionException::class.java)
                .hasMessage(AllocationErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 점유_이행 {

        @Test
        fun `HELD 상태의 점유는 FULFILLED로 전환된다`() {
            val allocation: Allocation = allocation().allocationId(1L).status(AllocationStatus.HELD).build()

            allocation.fulfill()

            assertThat(allocation.status).isEqualTo(AllocationStatus.FULFILLED)
        }

        @ParameterizedTest
        @EnumSource(value = AllocationStatus::class, names = ["RELEASED", "FULFILLED"])
        fun `이미 종결된 점유는 이행할 수 없다`(finalStatus: AllocationStatus) {
            val allocation: Allocation = allocation().allocationId(1L).status(finalStatus).build()

            assertThatThrownBy { allocation.fulfill() }
                .isInstanceOf(InvalidAllocationStatusTransitionException::class.java)
                .hasMessage(AllocationErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }
}
