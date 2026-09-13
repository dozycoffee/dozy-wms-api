package com.dozycoffee.wms.warehouse.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.InactiveWorkAreaException
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientWorkAreaCapacityException
import com.dozycoffee.wms.warehouse.domain.exception.InvalidWorkAreaAmountException
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaCapacityExceededException
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaErrorCode
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity
import com.dozycoffee.wms.warehouse.fixture.WorkAreaTestBuilder
import com.dozycoffee.wms.warehouse.fixture.WorkAreaTestBuilder.Companion.workArea
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class WorkAreaTest {

    @Nested
    inner class 작업구역_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 작업구역 객체가 올바르게 생성된다`() {
            val workArea: WorkArea = WorkAreaTestBuilder().build()

            assertThat(workArea.warehouseId).isEqualTo(1L)
            assertThat(workArea.areaCode).isEqualTo(AreaCode.INBOUND)
            assertThat(workArea.workAreaStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }

        @Test
        fun `생성 시 사용량은 0으로 초기화된다`() {
            val workArea: WorkArea = workArea().usedCapacity(30).build()

            assertThat(workArea.usedCapacity).isZero()
        }

        @Test
        fun `소속 창고 ID가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { workArea().warehouseId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(WorkAreaErrorCode.INVALID_WAREHOUSE_ID.message)
        }

        @Test
        fun `작업 구역 코드가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { workArea().areaCode(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(WorkAreaErrorCode.INVALID_AREA_CODE.message)
        }

        @Test
        fun `작업 구역 상태가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { workArea().workAreaStatus(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(WorkAreaErrorCode.INVALID_WORK_AREA_STATUS.message)
        }
    }

    @Nested
    inner class 작업구역_재구성 {

        @Test
        fun `저장된 ID와 사용량으로 작업구역 객체를 재구성한다`() {
            val workArea: WorkArea = workArea().workAreaId(10L).usedCapacity(20).build()

            assertThat(workArea.workAreaId).isEqualTo(10L)
            assertThat(workArea.usedCapacity).isEqualTo(20)
        }
    }

    @Nested
    inner class 작업_구역_코드_속성 {

        @Test
        fun `작업 구역 코드에 따라 구역명과 수용량이 함께 결정된다`() {
            val workArea: WorkArea = workArea().areaCode(AreaCode.INBOUND).build()

            assertThat(workArea.areaName).isEqualTo("입고 처리장")
            assertThat(workArea.capacity).isEqualTo(Capacity(50))
        }
    }

    @Nested
    inner class 재고_점유 {

        @Test
        fun `정상적으로 점유하면 사용량이 증가한다`() {
            val workArea: WorkArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(0).workAreaId(1L).build()

            workArea.occupy(20)

            assertThat(workArea.usedCapacity).isEqualTo(20)
        }

        @Test
        fun `최대 수용량을 초과하는 점유는 예외를 던진다`() {
            val workArea: WorkArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(40).workAreaId(1L).build()

            assertThatThrownBy { workArea.occupy(11) }
                .isInstanceOf(WorkAreaCapacityExceededException::class.java)
                .hasMessage(WorkAreaErrorCode.CAPACITY_EXCEEDED.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `점유 수량이 0 이하이면 예외를 던진다`(amount: Int) {
            val workArea: WorkArea = workArea().build()

            assertThatThrownBy { workArea.occupy(amount) }
                .isInstanceOf(InvalidWorkAreaAmountException::class.java)
                .hasMessage(WorkAreaErrorCode.INVALID_AMOUNT.message)
        }

        @Test
        fun `비활성화된 작업 구역은 점유할 수 없다`() {
            val workArea: WorkArea = workArea().workAreaStatus(AvailabilityStatus.UNAVAILABLE).build()

            assertThatThrownBy { workArea.occupy(10) }
                .isInstanceOf(InactiveWorkAreaException::class.java)
                .hasMessage(WorkAreaErrorCode.INACTIVE_WORK_AREA.message)
        }
    }

    @Nested
    inner class 재고_반출 {

        @Test
        fun `정상적으로 반출하면 사용량이 감소한다`() {
            val workArea: WorkArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(20).workAreaId(1L).build()

            workArea.release(20)

            assertThat(workArea.usedCapacity).isZero()
        }

        @Test
        fun `현재 사용량보다 많은 수량을 반출하면 예외를 던진다`() {
            val workArea: WorkArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(10).workAreaId(1L).build()

            assertThatThrownBy { workArea.release(11) }
                .isInstanceOf(InsufficientWorkAreaCapacityException::class.java)
                .hasMessage(WorkAreaErrorCode.INSUFFICIENT_USED_CAPACITY.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `반출 수량이 0 이하이면 예외를 던진다`(amount: Int) {
            val workArea: WorkArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(10).workAreaId(1L).build()

            assertThatThrownBy { workArea.release(amount) }
                .isInstanceOf(InvalidWorkAreaAmountException::class.java)
                .hasMessage(WorkAreaErrorCode.INVALID_AMOUNT.message)
        }

        @Test
        fun `비활성화된 작업 구역은 반출할 수 없다`() {
            val workArea: WorkArea = workArea()
                .usedCapacity(10)
                .workAreaId(1L)
                .workAreaStatus(AvailabilityStatus.UNAVAILABLE)
                .build()

            assertThatThrownBy { workArea.release(5) }
                .isInstanceOf(InactiveWorkAreaException::class.java)
                .hasMessage(WorkAreaErrorCode.INACTIVE_WORK_AREA.message)
        }
    }
}
