package com.dozycoffee.wms.stock_audit.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditItemErrorCode
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import com.dozycoffee.wms.stock_audit.fixture.StockAuditItemTestBuilder.Companion.stockAuditItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class StockAuditItemTest {

    @Nested
    inner class 실사_항목_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 생성되고 카운트 전 상태다`() {
            val item: StockAuditItem = stockAuditItem().snapshotQuantity(20).build()

            assertThat(item.stockAuditId).isEqualTo(1L)
            assertThat(item.inventoryId).isEqualTo(1L)
            assertThat(item.snapshotQuantity).isEqualTo(20)
            assertThat(item.isCounted).isFalse()
            assertThat(item.discrepancy).isNull()
            assertThat(item.hasUncommittedMovement).isFalse()
        }

        @Test
        fun `실사 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { stockAuditItem().stockAuditId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(StockAuditItemErrorCode.INVALID_STOCK_AUDIT_ID.message)
        }

        @Test
        fun `재고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { stockAuditItem().inventoryId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(StockAuditItemErrorCode.INVALID_INVENTORY_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, -10])
        fun `스냅샷 수량이 음수이면 예외를 던진다`(invalidQuantity: Int) {
            assertThatThrownBy { stockAuditItem().snapshotQuantity(invalidQuantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(StockAuditItemErrorCode.INVALID_SNAPSHOT_QUANTITY.message)
        }
    }

    @Nested
    inner class 실사_항목_재구성 {

        @Test
        fun `저장된 ID와 실사 결과로 재구성한다`() {
            val item: StockAuditItem = stockAuditItem()
                .stockAuditItemId(100L).snapshotQuantity(20).countedQuantity(15).hasUncommittedMovement(true).build()

            assertThat(item.stockAuditItemId).isEqualTo(100L)
            assertThat(item.countedQuantity).isEqualTo(15)
            assertThat(item.discrepancy).isEqualTo(-5)
            assertThat(item.hasUncommittedMovement).isTrue()
        }
    }

    @Nested
    inner class 실측_수량_입력 {

        @Test
        fun `실측 수량을 입력하면 카운트 상태가 되고 스냅샷 기준 차이가 계산된다`() {
            val item: StockAuditItem = stockAuditItem().snapshotQuantity(20).build()

            item.count(18)

            assertThat(item.isCounted).isTrue()
            assertThat(item.countedQuantity).isEqualTo(18)
            assertThat(item.discrepancy).isEqualTo(-2)
        }

        @Test
        fun `재입력하면 이전 카운트 값을 덮어쓴다`() {
            val item: StockAuditItem = stockAuditItem().snapshotQuantity(20).build()
            item.count(18)

            item.count(20)

            assertThat(item.countedQuantity).isEqualTo(20)
            assertThat(item.discrepancy).isEqualTo(0)
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, -10])
        fun `실측 수량이 음수이면 예외를 던진다`(invalidQuantity: Int) {
            val item: StockAuditItem = stockAuditItem().build()

            assertThatThrownBy { item.count(invalidQuantity) }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(StockAuditItemErrorCode.INVALID_COUNTED_QUANTITY.message)
        }
    }

    @Nested
    inner class 미반영_이력_표시 {

        @Test
        fun `호출하면 hasUncommittedMovement가 true로 바뀐다`() {
            val item: StockAuditItem = stockAuditItem().build()

            item.markHasUncommittedMovement()

            assertThat(item.hasUncommittedMovement).isTrue()
        }
    }
}
