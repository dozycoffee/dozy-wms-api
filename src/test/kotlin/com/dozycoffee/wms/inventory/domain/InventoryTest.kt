package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InsufficientAvailableQuantityException
import com.dozycoffee.wms.inventory.domain.exception.InsufficientHeldQuantityException
import com.dozycoffee.wms.inventory.domain.exception.InvalidInventoryAmountException
import com.dozycoffee.wms.inventory.domain.exception.InventoryErrorCode
import com.dozycoffee.wms.inventory.domain.exception.InventoryHasActiveAllocationException
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotAllocatableException
import com.dozycoffee.wms.inventory.domain.model.Inventory
import com.dozycoffee.wms.inventory.fixture.InventoryTestBuilder.Companion.inventory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class InventoryTest {

    @Nested
    inner class 재고_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 정상 품질의 미점유 재고로 생성된다`() {
            val inventory: Inventory = inventory().build()

            assertThat(inventory.productId).isEqualTo(1L)
            assertThat(inventory.lotId).isEqualTo(1L)
            assertThat(inventory.locationId).isEqualTo(1L)
            assertThat(inventory.quantity).isEqualTo(10)
            assertThat(inventory.allocatedQuantity).isEqualTo(0)
            assertThat(inventory.availableQuantity).isEqualTo(10)
            assertThat(inventory.qualityStatus).isEqualTo(QualityStatus.NORMAL)
        }

        @Test
        fun `상품 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inventory().productId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_PRODUCT_ID.message)
        }

        @Test
        fun `Lot ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inventory().lotId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_LOT_ID.message)
        }

        @Test
        fun `위치 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inventory().locationId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_LOCATION_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `수량이 0 이하이면 예외를 던진다`(invalidQuantity: Int) {
            assertThatThrownBy { inventory().quantity(invalidQuantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_QUANTITY.message)
        }
    }

    @Nested
    inner class 재고_재구성 {

        @Test
        fun `저장된 ID와 상태로 재고 객체를 재구성한다`() {
            val inventory: Inventory = inventory()
                .inventoryId(100L)
                .quantity(100)
                .allocatedQuantity(50)
                .qualityStatus(QualityStatus.DEFECTIVE)
                .build()

            assertThat(inventory.inventoryId).isEqualTo(100L)
            assertThat(inventory.quantity).isEqualTo(100)
            assertThat(inventory.allocatedQuantity).isEqualTo(50)
            assertThat(inventory.availableQuantity).isEqualTo(50)
            assertThat(inventory.qualityStatus).isEqualTo(QualityStatus.DEFECTIVE)
        }
    }

    @Nested
    inner class 점유 {

        @Test
        fun `정상 품질의 재고 중 일부만 점유할 수 있다`() {
            val inventory: Inventory = inventory().inventoryId(1L).quantity(100).build()

            inventory.hold(50)

            assertThat(inventory.allocatedQuantity).isEqualTo(50)
            assertThat(inventory.availableQuantity).isEqualTo(50)
        }

        @ParameterizedTest
        @EnumSource(value = QualityStatus::class, names = ["DEFECTIVE", "DISPOSAL_SCHEDULED"])
        fun `정상 품질이 아니면 점유할 수 없다`(invalidQualityStatus: QualityStatus) {
            val inventory: Inventory = inventory().inventoryId(1L).qualityStatus(invalidQualityStatus).build()

            assertThatThrownBy { inventory.hold(1) }
                .isInstanceOf(InventoryNotAllocatableException::class.java)
                .hasMessage(InventoryErrorCode.NOT_ALLOCATABLE.message)
        }

        @Test
        fun `가용 수량보다 많이 점유하려 하면 예외를 던진다`() {
            val inventory: Inventory = inventory().inventoryId(1L).quantity(100).allocatedQuantity(60).build()

            assertThatThrownBy { inventory.hold(41) }
                .isInstanceOf(InsufficientAvailableQuantityException::class.java)
                .hasMessage(InventoryErrorCode.INSUFFICIENT_AVAILABLE_QUANTITY.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `점유 수량이 0 이하이면 예외를 던진다`(invalidAmount: Int) {
            val inventory: Inventory = inventory().inventoryId(1L).build()

            assertThatThrownBy { inventory.hold(invalidAmount) }
                .isInstanceOf(InvalidInventoryAmountException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_AMOUNT.message)
        }
    }

    @Nested
    inner class 점유_해제 {

        @Test
        fun `점유를 해제하면 가용 수량으로 되돌아간다`() {
            val inventory: Inventory = inventory().inventoryId(1L).quantity(100).allocatedQuantity(50).build()

            inventory.releaseHold(30)

            assertThat(inventory.allocatedQuantity).isEqualTo(20)
            assertThat(inventory.availableQuantity).isEqualTo(80)
        }

        @Test
        fun `점유된 수량보다 많이 해제하려 하면 예외를 던진다`() {
            val inventory: Inventory = inventory().inventoryId(1L).quantity(100).allocatedQuantity(30).build()

            assertThatThrownBy { inventory.releaseHold(31) }
                .isInstanceOf(InsufficientHeldQuantityException::class.java)
                .hasMessage(InventoryErrorCode.INSUFFICIENT_HELD_QUANTITY.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `해제 수량이 0 이하이면 예외를 던진다`(invalidAmount: Int) {
            val inventory: Inventory = inventory().inventoryId(1L).allocatedQuantity(10).build()

            assertThatThrownBy { inventory.releaseHold(invalidAmount) }
                .isInstanceOf(InvalidInventoryAmountException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_AMOUNT.message)
        }
    }

    @Nested
    inner class 점유_이행 {

        @Test
        fun `점유를 이행하면 총 수량과 점유 수량이 함께 줄어든다`() {
            val inventory: Inventory = inventory().inventoryId(1L).quantity(100).allocatedQuantity(50).build()

            inventory.fulfillHold(30)

            assertThat(inventory.quantity).isEqualTo(70)
            assertThat(inventory.allocatedQuantity).isEqualTo(20)
            assertThat(inventory.availableQuantity).isEqualTo(50)
        }

        @Test
        fun `점유된 수량보다 많이 이행하려 하면 예외를 던진다`() {
            val inventory: Inventory = inventory().inventoryId(1L).quantity(100).allocatedQuantity(30).build()

            assertThatThrownBy { inventory.fulfillHold(31) }
                .isInstanceOf(InsufficientHeldQuantityException::class.java)
                .hasMessage(InventoryErrorCode.INSUFFICIENT_HELD_QUANTITY.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `이행 수량이 0 이하이면 예외를 던진다`(invalidAmount: Int) {
            val inventory: Inventory = inventory().inventoryId(1L).allocatedQuantity(10).build()

            assertThatThrownBy { inventory.fulfillHold(invalidAmount) }
                .isInstanceOf(InvalidInventoryAmountException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_AMOUNT.message)
        }
    }

    @Nested
    inner class 불량_판정 {

        @Test
        fun `점유가 없는 재고를 불량으로 판정하면 품질 상태가 DEFECTIVE로 바뀐다`() {
            val inventory: Inventory = inventory().build()

            inventory.markDefective()

            assertThat(inventory.qualityStatus).isEqualTo(QualityStatus.DEFECTIVE)
        }

        @Test
        fun `점유가 남아있는 재고를 불량으로 판정하면 예외를 던진다`() {
            val inventory: Inventory = inventory().inventoryId(1L).allocatedQuantity(1).build()

            assertThatThrownBy { inventory.markDefective() }
                .isInstanceOf(InventoryHasActiveAllocationException::class.java)
                .hasMessage(InventoryErrorCode.HAS_ACTIVE_ALLOCATION.message)
        }
    }

    @Nested
    inner class 폐기_예정_전환 {

        @Test
        fun `점유가 없는 재고를 폐기 예정으로 전환하면 품질 상태가 DISPOSAL_SCHEDULED로 바뀐다`() {
            val inventory: Inventory = inventory().build()

            inventory.markDisposalScheduled()

            assertThat(inventory.qualityStatus).isEqualTo(QualityStatus.DISPOSAL_SCHEDULED)
        }

        @Test
        fun `점유가 남아있는 재고를 폐기 예정으로 전환하면 예외를 던진다`() {
            val inventory: Inventory = inventory().inventoryId(1L).allocatedQuantity(1).build()

            assertThatThrownBy { inventory.markDisposalScheduled() }
                .isInstanceOf(InventoryHasActiveAllocationException::class.java)
                .hasMessage(InventoryErrorCode.HAS_ACTIVE_ALLOCATION.message)
        }
    }

    @Nested
    inner class 재고_삭제 {

        @Test
        fun `재고를 삭제하면 isDeleted가 true가 된다`() {
            val inventory: Inventory = inventory().inventoryId(1L).build()

            inventory.delete("system")

            assertThat(inventory.isDeleted()).isTrue()
            assertThat(inventory.deletedBy).isEqualTo("system")
        }
    }
}
