package com.dozycoffee.wms.warehouse.domain;

import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.InactiveLocationException;
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException;
import com.dozycoffee.wms.warehouse.domain.exception.InvalidLocationAmountException;
import com.dozycoffee.wms.warehouse.domain.exception.InvalidCapacityException;
import com.dozycoffee.wms.warehouse.domain.exception.InvalidLocationCodeException;
import com.dozycoffee.wms.warehouse.domain.exception.LocationCapacityExceededException;
import com.dozycoffee.wms.warehouse.domain.exception.LocationErrorCode;
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode;
import com.dozycoffee.wms.warehouse.domain.model.Location;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import com.dozycoffee.wms.warehouse.domain.valueobject.LocationCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.location;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LocationTest {

    @Nested
    class 위치_생성 {

        @Test
        void 정상적인_정보를_입력했을때_위치_객체가_올바르게_생성된다() {
            Location location = location().build();

            assertThat(location.getZoneId()).isEqualTo(1L);
            assertThat(location.getLocationCode()).isEqualTo(new LocationCode("A-01"));
            assertThat(location.getMaxCapacity()).isEqualTo(new Capacity(70));
            assertThat(location.getLocationStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        }

        @Test
        void 생성_시_사용량은_0으로_초기화된다() {
            Location location = location().usedCapacity(30).build();

            assertThat(location.getUsedCapacity()).isZero();
        }

        @Test
        void 소속_구역_ID가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    location().zoneId(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(LocationErrorCode.INVALID_ZONE_ID.getMessage());
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "A", "A-1", "A-001", "a-01", "AB-01", "1-01"})
        void 위치_코드가_형식에_맞지_않으면_예외를_던진다(String invalidLocationCode) {
            assertThatThrownBy(() ->
                    location().locationCode(invalidLocationCode).build()
            )
                    .isInstanceOf(InvalidLocationCodeException.class)
                    .hasMessage(LocationErrorCode.INVALID_LOCATION_CODE.getMessage());
        }

        @Test
        void 최대_수용_수량이_음수인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    location().maxCapacity(-1).build()
            )
                    .isInstanceOf(InvalidCapacityException.class)
                    .hasMessage(WarehouseErrorCode.INVALID_CAPACITY.getMessage());
        }

        @Test
        void 위치_상태가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    location().locationStatus(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(LocationErrorCode.INVALID_LOCATION_STATUS.getMessage());
        }
    }

    @Nested
    class 위치_재구성 {

        @Test
        void 저장된_ID와_사용량으로_위치_객체를_재구성한다() {
            Location location = location().locationId(10L).usedCapacity(20).build();

            assertThat(location.getLocationId()).isEqualTo(10L);
            assertThat(location.getUsedCapacity()).isEqualTo(20);
        }
    }

    @Nested
    class 재고_적재 {

        @Test
        void 정상적으로_적재하면_사용량이_증가한다() {
            Location location = location().maxCapacity(70).usedCapacity(0).locationId(1L).build();

            location.occupy(20);

            assertThat(location.getUsedCapacity()).isEqualTo(20);
        }

        @Test
        void 최대_수용량을_초과하는_적재는_예외를_던진다() {
            Location location = location().maxCapacity(50).usedCapacity(40).locationId(1L).build();

            assertThatThrownBy(() -> location.occupy(11))
                    .isInstanceOf(LocationCapacityExceededException.class)
                    .hasMessage(LocationErrorCode.CAPACITY_EXCEEDED.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void 적재_수량이_0_이하이면_예외를_던진다(int amount) {
            Location location = location().build();

            assertThatThrownBy(() -> location.occupy(amount))
                    .isInstanceOf(InvalidLocationAmountException.class)
                    .hasMessage(LocationErrorCode.INVALID_AMOUNT.getMessage());
        }

        @Test
        void 비활성화된_위치는_적재할_수_없다() {
            Location location = location().locationStatus(AvailabilityStatus.UNAVAILABLE).build();

            assertThatThrownBy(() -> location.occupy(10))
                    .isInstanceOf(InactiveLocationException.class)
                    .hasMessage(LocationErrorCode.INACTIVE_LOCATION.getMessage());
        }
    }

    @Nested
    class 재고_반출 {

        @Test
        void 정상적으로_반출하면_사용량이_감소한다() {
            Location location = location().usedCapacity(20).locationId(1L).build();

            location.release(20);

            assertThat(location.getUsedCapacity()).isZero();
        }

        @Test
        void 현재_사용량보다_많은_수량을_반출하면_예외를_던진다() {
            Location location = location().usedCapacity(10).locationId(1L).build();

            assertThatThrownBy(() -> location.release(11))
                    .isInstanceOf(InsufficientLocationCapacityException.class)
                    .hasMessage(LocationErrorCode.INSUFFICIENT_USED_CAPACITY.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void 반출_수량이_0_이하이면_예외를_던진다(int amount) {
            Location location = location().usedCapacity(10).locationId(1L).build();

            assertThatThrownBy(() -> location.release(amount))
                    .isInstanceOf(InvalidLocationAmountException.class)
                    .hasMessage(LocationErrorCode.INVALID_AMOUNT.getMessage());
        }

        @Test
        void 비활성화된_위치는_반출할_수_없다() {
            Location location = location()
                    .usedCapacity(10)
                    .locationId(1L)
                    .locationStatus(AvailabilityStatus.UNAVAILABLE)
                    .build();

            assertThatThrownBy(() -> location.release(5))
                    .isInstanceOf(InactiveLocationException.class)
                    .hasMessage(LocationErrorCode.INACTIVE_LOCATION.getMessage());
        }
    }
}
