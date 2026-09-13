package com.dozycoffee.wms.warehouse.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

class WarehouseTest {

    @Nested
    inner class 창고_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 창고 객체가 활성 상태로 올바르게 생성된다`() {
            val warehouse: Warehouse = WarehouseTestBuilder().build()

            assertThat(warehouse.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = [" ", "    "])
        fun `창고명이 null이거나 공백인 경우 예외를 던진다`(invalidWarehouseName: String?) {
            assertThatThrownBy { WarehouseTestBuilder().warehouseName(invalidWarehouseName).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(WarehouseErrorCode.INVALID_WAREHOUSE_NAME.message)
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = [" ", "   "])
        fun `창고 주소가 null이거나 공백인 경우 예외를 던진다`(invalidAddress: String?) {
            assertThatThrownBy { WarehouseTestBuilder().address(invalidAddress).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(WarehouseErrorCode.INVALID_ADDRESS.message)
        }

        @Test
        fun `창고 상태가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { warehouse().warehouseStatus(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(WarehouseErrorCode.INVALID_WAREHOUSE_STATUS.message)
        }
    }

    @Nested
    inner class 창고_상태 {

        @Test
        fun `창고를 활성화하면 상태가 AVAILABLE로 변경된다`() {
            val warehouse: Warehouse = warehouse().warehouseStatus(AvailabilityStatus.UNAVAILABLE).build()

            warehouse.activate()

            assertThat(warehouse.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }

        @Test
        fun `창고를 비활성화하면 상태가 UNAVAILABLE로 변경된다`() {
            val warehouse: Warehouse = warehouse().warehouseStatus(AvailabilityStatus.AVAILABLE).build()

            warehouse.deactivate()

            assertThat(warehouse.warehouseStatus).isEqualTo(AvailabilityStatus.UNAVAILABLE)
        }
    }
}
