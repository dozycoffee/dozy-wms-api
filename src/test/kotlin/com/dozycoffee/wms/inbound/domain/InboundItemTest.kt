package com.dozycoffee.wms.inbound.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inbound.domain.exception.InboundItemAlreadyInspectedException
import com.dozycoffee.wms.inbound.domain.exception.InboundItemErrorCode
import com.dozycoffee.wms.inbound.domain.exception.InboundOverReceivedException
import com.dozycoffee.wms.inbound.domain.exception.InboundReceiptErrorCode
import com.dozycoffee.wms.inbound.domain.exception.LotExpirationConflictException
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
import com.dozycoffee.wms.inbound.fixture.InboundReceiptTestBuilder.Companion.inboundReceipt
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class InboundItemTest {

    @Nested
    inner class 입고_상품_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 PENDING 상태로 생성된다`() {
            val item: InboundItem = inboundItem().build()

            assertThat(item.inboundId).isEqualTo(1L)
            assertThat(item.productId).isEqualTo(1L)
            assertThat(item.zoneId).isEqualTo(1L)
            assertThat(item.expectedQuantity).isEqualTo(10)
            assertThat(item.actualQuantity).isNull()
            assertThat(item.inspectionStatus).isEqualTo(InspectionStatus.PENDING)
            assertThat(item.quantityDiscrepancy).isNull()
        }

        @Test
        fun `예정 로트 정보는 선택이며 공백 로트 번호는 없는 것으로 본다`() {
            val withLot: InboundItem = inboundItem().expectedLotNumber("LOT-9").expectedExpirationDate(LocalDate.of(2027, 1, 1)).build()
            val blankLot: InboundItem = inboundItem().expectedLotNumber(" ").build()

            assertThat(withLot.expectedLotNumber).isEqualTo("LOT-9")
            assertThat(withLot.expectedExpirationDate).isEqualTo(LocalDate.of(2027, 1, 1))
            assertThat(blankLot.expectedLotNumber).isNull()
        }

        @Test
        fun `입고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundItem().inboundId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_INBOUND_ID.message)
        }

        @Test
        fun `상품 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundItem().productId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_PRODUCT_ID.message)
        }

        @Test
        fun `Zone ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundItem().zoneId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_ZONE_ID.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1, -10])
        fun `입고 예정 수량이 0 이하이면 예외를 던진다`(invalidQuantity: Int) {
            assertThatThrownBy { inboundItem().expectedQuantity(invalidQuantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundItemErrorCode.INVALID_EXPECTED_QUANTITY.message)
        }
    }

    @Nested
    inner class 입고_상품_재구성 {

        @Test
        fun `저장된 ID와 검수 상태로 입고 상품을 재구성한다`() {
            val item: InboundItem = inboundItem()
                .inboundItemId(100L)
                .actualQuantity(8)
                .inspectionStatus(InspectionStatus.INSPECTED)
                .build()

            assertThat(item.inboundItemId).isEqualTo(100L)
            assertThat(item.actualQuantity).isEqualTo(8)
            assertThat(item.inspectionStatus).isEqualTo(InspectionStatus.INSPECTED)
        }
    }

    @Nested
    inner class 검수 {

        @Test
        fun `수령 라인 수량 합이 실제 수량으로 기록되고 검수 완료 상태가 된다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            item.inspect(listOf(inboundReceipt().quantity(10).build()))

            assertThat(item.actualQuantity).isEqualTo(10)
            assertThat(item.inspectionStatus).isEqualTo(InspectionStatus.INSPECTED)
            assertThat(item.quantityDiscrepancy).isZero()
        }

        @Test
        fun `한 상품에 로트가 여러 개이면 수량이 합산된다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            item.inspect(
                listOf(
                    inboundReceipt().lotNumber("LOT-A").quantity(6).build(),
                    inboundReceipt().lotNumber("LOT-B").quantity(4).build()
                )
            )

            assertThat(item.actualQuantity).isEqualTo(10)
        }

        @Test
        fun `같은 로트의 정상 라인과 불량 라인을 함께 기록할 수 있다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            item.inspect(
                listOf(
                    inboundReceipt().quantity(8).normal().build(),
                    inboundReceipt().quantity(2).defective(DefectReason.DAMAGED).build()
                )
            )

            assertThat(item.actualQuantity).isEqualTo(10)
        }

        @Test
        fun `수령 합계가 예정 수량보다 적으면 차이가 기록된다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            item.inspect(listOf(inboundReceipt().quantity(7).build()))

            assertThat(item.quantityDiscrepancy).isEqualTo(-3)
        }

        @Test
        fun `수령 라인이 없으면 미도착으로 수량 0이 기록된다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            item.inspect(emptyList())

            assertThat(item.actualQuantity).isZero()
            assertThat(item.inspectionStatus).isEqualTo(InspectionStatus.INSPECTED)
            assertThat(item.quantityDiscrepancy).isEqualTo(-10)
        }

        @Test
        fun `수령 합계가 예정 수량을 초과하면 예외를 던진다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            assertThatThrownBy {
                item.inspect(
                    listOf(
                        inboundReceipt().lotNumber("LOT-A").quantity(6).build(),
                        inboundReceipt().lotNumber("LOT-B").quantity(5).build()
                    )
                )
            }
                .isInstanceOf(InboundOverReceivedException::class.java)
                .hasMessage(InboundReceiptErrorCode.OVER_RECEIVED.message)
            assertThat(item.inspectionStatus).isEqualTo(InspectionStatus.PENDING)
        }

        @Test
        fun `같은 로트 번호에 서로 다른 유통기한이 있으면 예외를 던진다`() {
            val item: InboundItem = inboundItem().inboundItemId(1L).expectedQuantity(10).build()

            assertThatThrownBy {
                item.inspect(
                    listOf(
                        inboundReceipt().lotNumber("LOT-A").expirationDate(LocalDate.of(2027, 1, 1)).quantity(5).build(),
                        inboundReceipt().lotNumber("LOT-A").expirationDate(LocalDate.of(2027, 2, 1)).quantity(5).build()
                    )
                )
            }.isInstanceOf(LotExpirationConflictException::class.java)
        }

        @Test
        fun `이미 검수된 상품을 다시 검수하면 예외를 던진다`() {
            val item: InboundItem = inboundItem()
                .inboundItemId(1L)
                .actualQuantity(10)
                .inspectionStatus(InspectionStatus.INSPECTED)
                .build()

            assertThatThrownBy { item.inspect(listOf(inboundReceipt().build())) }
                .isInstanceOf(InboundItemAlreadyInspectedException::class.java)
                .hasMessage(InboundItemErrorCode.ALREADY_INSPECTED.message)
        }
    }
}
