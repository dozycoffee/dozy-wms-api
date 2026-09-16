package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.exception.InventoryHistoryErrorCode
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import com.dozycoffee.wms.inventory.fixture.InventoryHistoryTestBuilder.Companion.inventoryHistory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class InventoryHistoryTest {

    @Nested
    inner class 이력_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 이력이 생성된다`() {
            val history: InventoryHistory = inventoryHistory()
                .inventoryId(1L).historyType(InventoryHistoryType.INBOUND).quantityChange(10).referenceId(100L)
                .build()

            assertThat(history.inventoryId).isEqualTo(1L)
            assertThat(history.historyType).isEqualTo(InventoryHistoryType.INBOUND)
            assertThat(history.quantityChange).isEqualTo(10)
            assertThat(history.referenceId).isEqualTo(100L)
        }

        @Test
        fun `재고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inventoryHistory().inventoryId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InventoryHistoryErrorCode.INVALID_INVENTORY_ID.message)
        }

        @Test
        fun `이력 유형이 null이면 예외를 던진다`() {
            assertThatThrownBy { inventoryHistory().historyType(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InventoryHistoryErrorCode.INVALID_HISTORY_TYPE.message)
        }

        @Test
        fun `참조 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inventoryHistory().referenceId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InventoryHistoryErrorCode.INVALID_REFERENCE_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0])
        fun `변화량이 0이면 예외를 던진다`(invalidQuantityChange: Int) {
            assertThatThrownBy { inventoryHistory().quantityChange(invalidQuantityChange).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InventoryHistoryErrorCode.INVALID_QUANTITY_CHANGE.message)
        }

        @Test
        fun `변화량이 음수여도 생성된다 (출고 폐기 등 감소 이력)`() {
            val history: InventoryHistory = inventoryHistory().quantityChange(-10).build()

            assertThat(history.quantityChange).isEqualTo(-10)
        }
    }

    @Nested
    inner class 이력_재구성 {

        @Test
        fun `저장된 ID로 이력 객체를 재구성한다`() {
            val history: InventoryHistory = inventoryHistory().inventoryHistoryId(100L).build()

            assertThat(history.inventoryHistoryId).isEqualTo(100L)
        }
    }
}
