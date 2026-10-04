package com.dozycoffee.wms.inbound.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.ExpiredReceiptCannotBeNormalException
import com.dozycoffee.wms.inbound.domain.exception.InboundReceiptErrorCode
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import com.dozycoffee.wms.inbound.fixture.InboundReceiptTestBuilder.Companion.inboundReceipt
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class InboundReceiptTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 4)

    @Nested
    inner class 수령_라인_생성 {

        @Test
        fun `정상 라인은 사유 없이 생성된다`() {
            val receipt: InboundReceipt = inboundReceipt()
                .lotNumber("LOT-1").quantity(5)
                .manufactureDate(today.minusDays(10)).expirationDate(today.plusDays(100))
                .build()

            assertThat(receipt.lotNumber).isEqualTo("LOT-1")
            assertThat(receipt.quantity).isEqualTo(5)
            assertThat(receipt.inspectionResult).isEqualTo(InspectionResult.NORMAL)
            assertThat(receipt.defectReason).isNull()
        }

        @Test
        fun `불량 라인은 사유와 함께 생성된다`() {
            val receipt: InboundReceipt = inboundReceipt().defective(DefectReason.QUALITY).build()

            assertThat(receipt.inspectionResult).isEqualTo(InspectionResult.DEFECTIVE)
            assertThat(receipt.defectReason).isEqualTo(DefectReason.QUALITY)
        }

        @Test
        fun `유통기한 없는 상품의 라인도 생성된다`() {
            val receipt: InboundReceipt = inboundReceipt().expirationDate(null).build()

            assertThat(receipt.expirationDate).isNull()
        }

        @Test
        fun `입고 상품 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundReceipt().inboundItemId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.INVALID_INBOUND_ITEM_ID.message)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", " "])
        fun `로트 번호가 비어 있으면 예외를 던진다`(blank: String) {
            assertThatThrownBy { inboundReceipt().lotNumber(blank).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.INVALID_LOT_NUMBER.message)
        }

        @Test
        fun `로트 번호가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundReceipt().lotNumber(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.INVALID_LOT_NUMBER.message)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `수량이 0 이하이면 예외를 던진다`(quantity: Int) {
            assertThatThrownBy { inboundReceipt().quantity(quantity).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.INVALID_QUANTITY.message)
        }

        @Test
        fun `검수 결과가 null이면 예외를 던진다`() {
            assertThatThrownBy { inboundReceipt().inspectionResult(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.INVALID_INSPECTION_RESULT.message)
        }
    }

    @Nested
    inner class 불량_사유 {

        @Test
        fun `불량 판정에 사유가 없으면 예외를 던진다`() {
            assertThatThrownBy { inboundReceipt().inspectionResult(InspectionResult.DEFECTIVE).defectReason(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.DEFECT_REASON_REQUIRED.message)
        }

        @Test
        fun `정상 판정에 사유가 있으면 예외를 던진다`() {
            assertThatThrownBy { inboundReceipt().inspectionResult(InspectionResult.NORMAL).defectReason(DefectReason.DAMAGED).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.DEFECT_REASON_NOT_ALLOWED.message)
        }
    }

    @Nested
    inner class 날짜_규칙 {

        @Test
        fun `제조일자가 유통기한보다 늦으면 예외를 던진다`() {
            assertThatThrownBy {
                inboundReceipt().manufactureDate(today.minusDays(1)).expirationDate(today.minusDays(5)).defective(DefectReason.EXPIRED).build()
            }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.INVALID_DATE_RANGE.message)
        }

        @Test
        fun `제조일자가 미래이면 예외를 던진다`() {
            assertThatThrownBy { inboundReceipt().manufactureDate(today.plusDays(1)).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundReceiptErrorCode.MANUFACTURE_DATE_IN_FUTURE.message)
        }

        @Test
        fun `제조일자가 오늘이어도 생성된다`() {
            val receipt: InboundReceipt = inboundReceipt().manufactureDate(today).expirationDate(today.plusDays(30)).build()

            assertThat(receipt.manufactureDate).isEqualTo(today)
        }
    }

    @Nested
    inner class 유통기한_경과 {

        @Test
        fun `유통기한이 지난 라인을 정상으로 판정하면 예외를 던진다`() {
            assertThatThrownBy { inboundReceipt().expirationDate(today.minusDays(1)).normal().build() }
                .isInstanceOf(ExpiredReceiptCannotBeNormalException::class.java)
                .hasMessage(InboundReceiptErrorCode.EXPIRED_CANNOT_BE_NORMAL.message)
        }

        @Test
        fun `유통기한이 지난 라인도 불량으로는 기록할 수 있다`() {
            val receipt: InboundReceipt = inboundReceipt().expirationDate(today.minusDays(1)).defective(DefectReason.EXPIRED).build()

            assertThat(receipt.defectReason).isEqualTo(DefectReason.EXPIRED)
        }

        @Test
        fun `유통기한이 오늘이면 정상으로 판정할 수 있다`() {
            val receipt: InboundReceipt = inboundReceipt().expirationDate(today).normal().build()

            assertThat(receipt.inspectionResult).isEqualTo(InspectionResult.NORMAL)
        }

        @Test
        fun `임박 유통기한은 정상으로 판정할 수 있다`() {
            val receipt: InboundReceipt = inboundReceipt().expirationDate(today.plusDays(5)).normal().build()

            assertThat(receipt.inspectionResult).isEqualTo(InspectionResult.NORMAL)
        }
    }
}
