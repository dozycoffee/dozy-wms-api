package com.dozycoffee.wms.warehouse.domain;

import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.enumeration.WarehouseStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode;
import com.dozycoffee.wms.warehouse.domain.model.Warehouse;
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WarehouseTest {

    @Nested
    class 창고_생성 {

        @Test
        void 정상적인_정보를_입력했을때_창고_객체가_활성_상태로_올바르게_생성된다() {
            Warehouse warehouse = new WarehouseTestBuilder().build();

            assertThat(warehouse.getWarehouseStatus()).isEqualTo(WarehouseStatus.AVAILABLE);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "    "})
        void 창고명이_null이거나_공백인_경우_예외를_던진다(String invalidWarehouseName) {
            assertThatThrownBy(() ->
                    new WarehouseTestBuilder().warehouseName(invalidWarehouseName).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(WarehouseErrorCode.INVALID_WAREHOUSE_NAME.getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void 창고_주소가_null이거나_공백인_경우_예외를_던진다(String invalidAddress) {
            assertThatThrownBy(() ->
                    new WarehouseTestBuilder().address(invalidAddress).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(WarehouseErrorCode.INVALID_ADDRESS.getMessage());
        }

        @Test
        void 창고_상태가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    warehouse().warehouseStatus(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(WarehouseErrorCode.INVALID_WAREHOUSE_STATUS.getMessage());
        }
    }

    @Nested
    class 창고_상태 {

        @Test
        void 창고를_활성화하면_상태가_AVAILABLE로_변경된다() {
            Warehouse warehouse = warehouse().warehouseStatus(WarehouseStatus.UNAVAILABLE).build();

            warehouse.activate();

            assertThat(warehouse.getWarehouseStatus()).isEqualTo(WarehouseStatus.AVAILABLE);
        }

        @Test
        void 창고를_비활성화하면_상태가_UNAVAILABLE로_변경된다() {
            Warehouse warehouse = warehouse().warehouseStatus(WarehouseStatus.AVAILABLE).build();

            warehouse.deactivate();

            assertThat(warehouse.getWarehouseStatus()).isEqualTo(WarehouseStatus.UNAVAILABLE);
        }
    }
}
