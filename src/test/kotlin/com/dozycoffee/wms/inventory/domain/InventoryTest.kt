package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InvalidInventoryStatusCombinationException
import com.dozycoffee.wms.inventory.domain.exception.InventoryErrorCode
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
        fun `정상적인 정보를 입력했을 때 정상 품질의 가용 재고로 생성된다`() {
            val inventory: Inventory = inventory().build()

            assertThat(inventory.productId).isEqualTo(1L)
            assertThat(inventory.lotId).isEqualTo(1L)
            assertThat(inventory.locationId).isEqualTo(1L)
            assertThat(inventory.quantity).isEqualTo(10)
            assertThat(inventory.qualityStatus).isEqualTo(QualityStatus.NORMAL)
            assertThat(inventory.allocationStatus).isEqualTo(AllocationStatus.AVAILABLE)
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
                .qualityStatus(QualityStatus.DEFECTIVE)
                .allocationStatus(AllocationStatus.AVAILABLE)
                .build()

            assertThat(inventory.inventoryId).isEqualTo(100L)
            assertThat(inventory.qualityStatus).isEqualTo(QualityStatus.DEFECTIVE)
            assertThat(inventory.allocationStatus).isEqualTo(AllocationStatus.AVAILABLE)
        }

        @ParameterizedTest
        @EnumSource(value = QualityStatus::class, names = ["DEFECTIVE", "DISPOSAL_SCHEDULED"])
        fun `정상 품질이 아닌데 할당 상태로 재구성하면 예외를 던진다`(
            invalidQualityStatus: QualityStatus
        ) {
            assertThatThrownBy {
                inventory()
                    .inventoryId(100L)
                    .qualityStatus(invalidQualityStatus)
                    .allocationStatus(AllocationStatus.ALLOCATED)
                    .build()
            }.isInstanceOf(InvalidInventoryStatusCombinationException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_STATUS_COMBINATION.message)
        }
    }

    @Nested
    inner class 재고_할당 {

        @Test
        fun `정상 품질의 가용 재고를 할당하면 할당 상태로 바뀐다`() {
            val inventory: Inventory = inventory().qualityStatus(QualityStatus.NORMAL).build()

            inventory.allocate()

            assertThat(inventory.allocationStatus).isEqualTo(AllocationStatus.ALLOCATED)
        }

        @ParameterizedTest
        @EnumSource(value = QualityStatus::class, names = ["DEFECTIVE", "DISPOSAL_SCHEDULED"])
        fun `정상 품질이 아닌 재고를 할당하면 예외를 던진다`(invalidQualityStatus: QualityStatus) {
            val inventory: Inventory = inventory().inventoryId(1L).qualityStatus(invalidQualityStatus).build()

            assertThatThrownBy { inventory.allocate() }
                .isInstanceOf(InvalidInventoryStatusCombinationException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_STATUS_COMBINATION.message)
        }
    }

    @Nested
    inner class 할당_해제 {

        @Test
        fun `할당된 재고를 해제하면 가용 상태로 바뀐다`() {
            val inventory: Inventory = inventory()
                .inventoryId(1L)
                .allocationStatus(AllocationStatus.ALLOCATED)
                .build()

            inventory.release()

            assertThat(inventory.allocationStatus).isEqualTo(AllocationStatus.AVAILABLE)
        }
    }

    @Nested
    inner class 불량_판정 {

        @Test
        fun `가용 재고를 불량으로 판정하면 품질 상태가 DEFECTIVE로 바뀐다`() {
            val inventory: Inventory = inventory().build()

            inventory.markDefective()

            assertThat(inventory.qualityStatus).isEqualTo(QualityStatus.DEFECTIVE)
        }

        @Test
        fun `할당된 재고를 불량으로 판정하면 예외를 던진다`() {
            val inventory: Inventory = inventory()
                .inventoryId(1L)
                .allocationStatus(AllocationStatus.ALLOCATED)
                .build()

            assertThatThrownBy { inventory.markDefective() }
                .isInstanceOf(InvalidInventoryStatusCombinationException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_STATUS_COMBINATION.message)
        }
    }

    @Nested
    inner class 폐기_예정_전환 {

        @Test
        fun `가용 재고를 폐기 예정으로 전환하면 품질 상태가 DISPOSAL_SCHEDULED로 바뀐다`() {
            val inventory: Inventory = inventory().build()

            inventory.markDisposalScheduled()

            assertThat(inventory.qualityStatus).isEqualTo(QualityStatus.DISPOSAL_SCHEDULED)
        }

        @Test
        fun `할당된 재고를 폐기 예정으로 전환하면 예외를 던진다`() {
            val inventory: Inventory = inventory()
                .inventoryId(1L)
                .allocationStatus(AllocationStatus.ALLOCATED)
                .build()

            assertThatThrownBy { inventory.markDisposalScheduled() }
                .isInstanceOf(InvalidInventoryStatusCombinationException::class.java)
                .hasMessage(InventoryErrorCode.INVALID_STATUS_COMBINATION.message)
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
