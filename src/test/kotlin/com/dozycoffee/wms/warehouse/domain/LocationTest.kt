package com.dozycoffee.wms.warehouse.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.InactiveLocationException
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException
import com.dozycoffee.wms.warehouse.domain.exception.InvalidCapacityException
import com.dozycoffee.wms.warehouse.domain.exception.InvalidLocationAmountException
import com.dozycoffee.wms.warehouse.domain.exception.InvalidLocationCodeException
import com.dozycoffee.wms.warehouse.domain.exception.LocationCapacityExceededException
import com.dozycoffee.wms.warehouse.domain.exception.LocationErrorCode
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode
import com.dozycoffee.wms.warehouse.domain.model.Location
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity
import com.dozycoffee.wms.warehouse.domain.valueobject.LocationCode
import com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.Companion.location
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class LocationTest {

    @Nested
    inner class 위치_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 위치 객체가 올바르게 생성된다`() {
            val location: Location = location().build()

            assertThat(location.zoneId).isEqualTo(1L)
            assertThat(location.locationCode).isEqualTo(LocationCode.of("A-01"))
            assertThat(location.maxCapacity).isEqualTo(Capacity(70))
            assertThat(location.locationStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }

        @Test
        fun `생성 시 사용량은 0으로 초기화된다`() {
            val location: Location = location().usedCapacity(30).build()

            assertThat(location.usedCapacity).isZero()
        }

        @Test
        fun `소속 구역 ID가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { location().zoneId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(LocationErrorCode.INVALID_ZONE_ID.message)
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["", "A", "A-1", "A-001", "a-01", "AB-01", "1-01"])
        fun `위치 코드가 형식에 맞지 않으면 예외를 던진다`(invalidLocationCode: String?) {
            assertThatThrownBy { location().locationCode(invalidLocationCode).build() }
                .isInstanceOf(InvalidLocationCodeException::class.java)
                .hasMessage(LocationErrorCode.INVALID_LOCATION_CODE.message)
        }

        @Test
        fun `최대 수용 수량이 음수인 경우 예외를 던진다`() {
            assertThatThrownBy { location().maxCapacity(-1).build() }
                .isInstanceOf(InvalidCapacityException::class.java)
                .hasMessage(WarehouseErrorCode.INVALID_CAPACITY.message)
        }

        @Test
        fun `위치 상태가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { location().locationStatus(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(LocationErrorCode.INVALID_LOCATION_STATUS.message)
        }
    }

    @Nested
    inner class 위치_재구성 {

        @Test
        fun `저장된 ID와 사용량으로 위치 객체를 재구성한다`() {
            val location: Location = location().locationId(10L).usedCapacity(20).build()

            assertThat(location.locationId).isEqualTo(10L)
            assertThat(location.usedCapacity).isEqualTo(20)
        }
    }

    @Nested
    inner class 재고_적재 {

        @Test
        fun `정상적으로 적재하면 사용량이 증가한다`() {
            val location: Location = location().maxCapacity(70).usedCapacity(0).locationId(1L).build()

            location.occupy(20)

            assertThat(location.usedCapacity).isEqualTo(20)
        }

        @Test
        fun `최대 수용량을 초과하는 적재는 예외를 던진다`() {
            val location: Location = location().maxCapacity(50).usedCapacity(40).locationId(1L).build()

            assertThatThrownBy { location.occupy(11) }
                .isInstanceOf(LocationCapacityExceededException::class.java)
                .hasMessage(LocationErrorCode.CAPACITY_EXCEEDED.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `적재 수량이 0 이하이면 예외를 던진다`(amount: Int) {
            val location: Location = location().build()

            assertThatThrownBy { location.occupy(amount) }
                .isInstanceOf(InvalidLocationAmountException::class.java)
                .hasMessage(LocationErrorCode.INVALID_AMOUNT.message)
        }

        @Test
        fun `비활성화된 위치는 적재할 수 없다`() {
            val location: Location = location().locationStatus(AvailabilityStatus.UNAVAILABLE).build()

            assertThatThrownBy { location.occupy(10) }
                .isInstanceOf(InactiveLocationException::class.java)
                .hasMessage(LocationErrorCode.INACTIVE_LOCATION.message)
        }
    }

    @Nested
    inner class 재고_반출 {

        @Test
        fun `정상적으로 반출하면 사용량이 감소한다`() {
            val location: Location = location().usedCapacity(20).locationId(1L).build()

            location.release(20)

            assertThat(location.usedCapacity).isZero()
        }

        @Test
        fun `현재 사용량보다 많은 수량을 반출하면 예외를 던진다`() {
            val location: Location = location().usedCapacity(10).locationId(1L).build()

            assertThatThrownBy { location.release(11) }
                .isInstanceOf(InsufficientLocationCapacityException::class.java)
                .hasMessage(LocationErrorCode.INSUFFICIENT_USED_CAPACITY.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `반출 수량이 0 이하이면 예외를 던진다`(amount: Int) {
            val location: Location = location().usedCapacity(10).locationId(1L).build()

            assertThatThrownBy { location.release(amount) }
                .isInstanceOf(InvalidLocationAmountException::class.java)
                .hasMessage(LocationErrorCode.INVALID_AMOUNT.message)
        }

        @Test
        fun `비활성화된 위치는 반출할 수 없다`() {
            val location: Location = location()
                .usedCapacity(10)
                .locationId(1L)
                .locationStatus(AvailabilityStatus.UNAVAILABLE)
                .build()

            assertThatThrownBy { location.release(5) }
                .isInstanceOf(InactiveLocationException::class.java)
                .hasMessage(LocationErrorCode.INACTIVE_LOCATION.message)
        }
    }
}
