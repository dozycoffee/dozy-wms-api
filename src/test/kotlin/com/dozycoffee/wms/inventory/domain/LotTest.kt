package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.exception.InvalidLotStatusTransitionException
import com.dozycoffee.wms.inventory.domain.exception.LotErrorCode
import com.dozycoffee.wms.inventory.domain.model.Lot
import com.dozycoffee.wms.inventory.fixture.LotTestBuilder.Companion.lot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class LotTest {

    @Nested
    inner class Lot_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 NORMAL 상태로 생성된다`() {
            val lot: Lot = lot().build()

            assertThat(lot.lotNumber).isEqualTo("LOT-20260101-001")
            assertThat(lot.productId).isEqualTo(1L)
            assertThat(lot.manufactureDate).isEqualTo(LocalDate.of(2026, 1, 1))
            assertThat(lot.expirationDate).isEqualTo(LocalDate.of(2026, 12, 31))
            assertThat(lot.lotStatus).isEqualTo(LotStatus.NORMAL)
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = [" ", "   "])
        fun `Lot 번호가 null이거나 공백인 경우 예외를 던진다`(invalidLotNumber: String?) {
            assertThatThrownBy { lot().lotNumber(invalidLotNumber).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(LotErrorCode.INVALID_LOT_NUMBER.message)
        }

        @Test
        fun `상품 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { lot().productId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(LotErrorCode.INVALID_PRODUCT_ID.message)
        }

        @Test
        fun `유통기한이 제조일자보다 빠르면 예외를 던진다`() {
            assertThatThrownBy {
                lot()
                    .manufactureDate(LocalDate.of(2026, 1, 10))
                    .expirationDate(LocalDate.of(2026, 1, 1))
                    .build()
            }.isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(LotErrorCode.INVALID_EXPIRATION_DATE.message)
        }

        @Test
        fun `제조일자와 유통기한은 null을 허용한다`() {
            val lot: Lot = lot().manufactureDate(null).expirationDate(null).build()

            assertThat(lot.manufactureDate).isNull()
            assertThat(lot.expirationDate).isNull()
        }
    }

    @Nested
    inner class Lot_재구성 {

        @Test
        fun `저장된 ID와 상태로 Lot 객체를 재구성한다`() {
            val lot: Lot = lot().lotId(10L).lotStatus(LotStatus.EXPIRING_SOON).build()

            assertThat(lot.lotId).isEqualTo(10L)
            assertThat(lot.lotStatus).isEqualTo(LotStatus.EXPIRING_SOON)
        }
    }

    @Nested
    inner class 유통기한_임박_전환 {

        @Test
        fun `NORMAL 상태의 Lot은 EXPIRING_SOON으로 전환된다`() {
            val lot: Lot = lot().lotId(1L).lotStatus(LotStatus.NORMAL).build()

            lot.markExpiringSoon()

            assertThat(lot.lotStatus).isEqualTo(LotStatus.EXPIRING_SOON)
        }

        @ParameterizedTest
        @EnumSource(value = LotStatus::class, names = ["EXPIRING_SOON", "EXPIRED"])
        fun `NORMAL 상태가 아니면 예외를 던진다`(currentStatus: LotStatus) {
            val lot: Lot = lot().lotId(1L).lotStatus(currentStatus).build()

            assertThatThrownBy { lot.markExpiringSoon() }
                .isInstanceOf(InvalidLotStatusTransitionException::class.java)
                .hasMessage(LotErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 유통기한_경과_전환 {

        @ParameterizedTest
        @EnumSource(value = LotStatus::class, names = ["NORMAL", "EXPIRING_SOON"])
        fun `NORMAL 또는 EXPIRING_SOON 상태의 Lot은 EXPIRED로 전환된다`(currentStatus: LotStatus) {
            val lot: Lot = lot().lotId(1L).lotStatus(currentStatus).build()

            lot.markExpired()

            assertThat(lot.lotStatus).isEqualTo(LotStatus.EXPIRED)
        }

        @Test
        fun `이미 EXPIRED 상태이면 예외를 던진다`() {
            val lot: Lot = lot().lotId(1L).lotStatus(LotStatus.EXPIRED).build()

            assertThatThrownBy { lot.markExpired() }
                .isInstanceOf(InvalidLotStatusTransitionException::class.java)
                .hasMessage(LotErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }
}
