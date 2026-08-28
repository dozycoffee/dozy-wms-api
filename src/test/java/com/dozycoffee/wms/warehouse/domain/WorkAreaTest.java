package com.dozycoffee.wms.warehouse.domain;

import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.WorkAreaStatus;
import com.dozycoffee.wms.warehouse.domain.exception.InactiveWorkAreaException;
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientWorkAreaCapacityException;
import com.dozycoffee.wms.warehouse.domain.exception.InvalidWorkAreaAmountException;
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaCapacityExceededException;
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaErrorCode;
import com.dozycoffee.wms.warehouse.domain.model.WorkArea;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import com.dozycoffee.wms.warehouse.fixture.WorkAreaTestBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.dozycoffee.wms.warehouse.fixture.WorkAreaTestBuilder.workArea;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WorkAreaTest {

    @Nested
    class 작업구역_생성 {

        @Test
        void 정상적인_정보를_입력했을때_작업구역_객체가_올바르게_생성된다() {
            WorkArea workArea = new WorkAreaTestBuilder().build();

            assertThat(workArea.getWarehouseId()).isEqualTo(1L);
            assertThat(workArea.getAreaCode()).isEqualTo(AreaCode.INBOUND);
            assertThat(workArea.getWorkAreaStatus()).isEqualTo(WorkAreaStatus.ACTIVE);
        }

        @Test
        void 생성_시_사용량은_0으로_초기화된다() {
            WorkArea workArea = workArea().usedCapacity(30).build();

            assertThat(workArea.getUsedCapacity()).isZero();
        }

        @Test
        void 소속_창고_ID가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    workArea().warehouseId(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(WorkAreaErrorCode.INVALID_WAREHOUSE_ID.getMessage());
        }

        @Test
        void 작업_구역_코드가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    workArea().areaCode(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(WorkAreaErrorCode.INVALID_AREA_CODE.getMessage());
        }

        @Test
        void 작업_구역_상태가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    workArea().workAreaStatus(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(WorkAreaErrorCode.INVALID_WORK_AREA_STATUS.getMessage());
        }
    }

    @Nested
    class 작업구역_재구성 {

        @Test
        void 저장된_ID와_사용량으로_작업구역_객체를_재구성한다() {
            WorkArea workArea = workArea().workAreaId(10L).usedCapacity(20).build();

            assertThat(workArea.getWorkAreaId()).isEqualTo(10L);
            assertThat(workArea.getUsedCapacity()).isEqualTo(20);
        }
    }

    @Nested
    class 작업_구역_코드_속성 {

        @Test
        void 작업_구역_코드에_따라_구역명과_수용량이_함께_결정된다() {
            WorkArea workArea = workArea().areaCode(AreaCode.INBOUND).build();

            assertThat(workArea.getAreaName()).isEqualTo("입고 처리장");
            assertThat(workArea.getCapacity()).isEqualTo(new Capacity(50));
        }
    }

    @Nested
    class 재고_점유 {

        @Test
        void 정상적으로_점유하면_사용량이_증가한다() {
            WorkArea workArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(0).workAreaId(1L).build();

            workArea.occupy(20);

            assertThat(workArea.getUsedCapacity()).isEqualTo(20);
        }

        @Test
        void 최대_수용량을_초과하는_점유는_예외를_던진다() {
            WorkArea workArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(40).workAreaId(1L).build();

            assertThatThrownBy(() -> workArea.occupy(11))
                    .isInstanceOf(WorkAreaCapacityExceededException.class)
                    .hasMessage(WorkAreaErrorCode.CAPACITY_EXCEEDED.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void 점유_수량이_0_이하이면_예외를_던진다(int amount) {
            WorkArea workArea = workArea().build();

            assertThatThrownBy(() -> workArea.occupy(amount))
                    .isInstanceOf(InvalidWorkAreaAmountException.class)
                    .hasMessage(WorkAreaErrorCode.INVALID_AMOUNT.getMessage());
        }

        @Test
        void 비활성화된_작업_구역은_점유할_수_없다() {
            WorkArea workArea = workArea().workAreaStatus(WorkAreaStatus.INACTIVE).build();

            assertThatThrownBy(() -> workArea.occupy(10))
                    .isInstanceOf(InactiveWorkAreaException.class)
                    .hasMessage(WorkAreaErrorCode.INACTIVE_WORK_AREA.getMessage());
        }
    }

    @Nested
    class 재고_반출 {

        @Test
        void 정상적으로_반출하면_사용량이_감소한다() {
            WorkArea workArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(20).workAreaId(1L).build();

            workArea.release(20);

            assertThat(workArea.getUsedCapacity()).isZero();
        }

        @Test
        void 현재_사용량보다_많은_수량을_반출하면_예외를_던진다() {
            WorkArea workArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(10).workAreaId(1L).build();

            assertThatThrownBy(() -> workArea.release(11))
                    .isInstanceOf(InsufficientWorkAreaCapacityException.class)
                    .hasMessage(WorkAreaErrorCode.INSUFFICIENT_USED_CAPACITY.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void 반출_수량이_0_이하이면_예외를_던진다(int amount) {
            WorkArea workArea = workArea().areaCode(AreaCode.INBOUND).usedCapacity(10).workAreaId(1L).build();

            assertThatThrownBy(() -> workArea.release(amount))
                    .isInstanceOf(InvalidWorkAreaAmountException.class)
                    .hasMessage(WorkAreaErrorCode.INVALID_AMOUNT.getMessage());
        }

        @Test
        void 비활성화된_작업_구역은_반출할_수_없다() {
            WorkArea workArea = workArea()
                    .usedCapacity(10)
                    .workAreaId(1L)
                    .workAreaStatus(WorkAreaStatus.INACTIVE)
                    .build();

            assertThatThrownBy(() -> workArea.release(5))
                    .isInstanceOf(InactiveWorkAreaException.class)
                    .hasMessage(WorkAreaErrorCode.INACTIVE_WORK_AREA.getMessage());
        }
    }
}
