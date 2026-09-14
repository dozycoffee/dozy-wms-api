package com.dozycoffee.wms.inbound.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.exception.InboundErrorCode
import com.dozycoffee.wms.inbound.domain.exception.InvalidInboundStatusTransitionException
import com.dozycoffee.wms.inbound.domain.model.Inbound
import com.dozycoffee.wms.inbound.fixture.InboundTestBuilder.Companion.inbound
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class InboundTest {

    @Nested
    inner class 입고_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 EXPECTED 상태로 생성된다`() {
            val inbound: Inbound = inbound().build()

            assertThat(inbound.warehouseId).isEqualTo(1L)
            assertThat(inbound.expectedArrivalDate).isEqualTo(LocalDate.of(2026, 1, 1))
            assertThat(inbound.status).isEqualTo(InboundStatus.EXPECTED)
        }

        @Test
        fun `창고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { inbound().warehouseId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundErrorCode.INVALID_WAREHOUSE_ID.message)
        }

        @Test
        fun `입고 예정일이 null이면 예외를 던진다`() {
            assertThatThrownBy { inbound().expectedArrivalDate(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(InboundErrorCode.INVALID_EXPECTED_ARRIVAL_DATE.message)
        }
    }

    @Nested
    inner class 입고_재구성 {

        @Test
        fun `저장된 ID와 상태로 입고 객체를 재구성한다`() {
            val inbound: Inbound = inbound().inboundId(100L).status(InboundStatus.PROCESSING).build()

            assertThat(inbound.inboundId).isEqualTo(100L)
            assertThat(inbound.status).isEqualTo(InboundStatus.PROCESSING)
        }
    }

    @Nested
    inner class 입고_대기_전환 {

        @Test
        fun `EXPECTED 상태는 WAITING으로 전환된다`() {
            val inbound: Inbound = inbound().inboundId(1L).status(InboundStatus.EXPECTED).build()

            inbound.markWaiting()

            assertThat(inbound.status).isEqualTo(InboundStatus.WAITING)
        }

        @ParameterizedTest
        @EnumSource(value = InboundStatus::class, names = ["WAITING", "PROCESSING", "COMPLETED"])
        fun `EXPECTED 상태가 아니면 예외를 던진다`(currentStatus: InboundStatus) {
            val inbound: Inbound = inbound().inboundId(1L).status(currentStatus).build()

            assertThatThrownBy { inbound.markWaiting() }
                .isInstanceOf(InvalidInboundStatusTransitionException::class.java)
                .hasMessage(InboundErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 입고_처리_시작 {

        @Test
        fun `WAITING 상태는 PROCESSING으로 전환된다`() {
            val inbound: Inbound = inbound().inboundId(1L).status(InboundStatus.WAITING).build()

            inbound.startProcessing()

            assertThat(inbound.status).isEqualTo(InboundStatus.PROCESSING)
        }

        @ParameterizedTest
        @EnumSource(value = InboundStatus::class, names = ["EXPECTED", "PROCESSING", "COMPLETED"])
        fun `WAITING 상태가 아니면 예외를 던진다`(currentStatus: InboundStatus) {
            val inbound: Inbound = inbound().inboundId(1L).status(currentStatus).build()

            assertThatThrownBy { inbound.startProcessing() }
                .isInstanceOf(InvalidInboundStatusTransitionException::class.java)
                .hasMessage(InboundErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 입고_완료 {

        @Test
        fun `PROCESSING 상태는 COMPLETED로 전환된다`() {
            val inbound: Inbound = inbound().inboundId(1L).status(InboundStatus.PROCESSING).build()

            inbound.complete()

            assertThat(inbound.status).isEqualTo(InboundStatus.COMPLETED)
        }

        @ParameterizedTest
        @EnumSource(value = InboundStatus::class, names = ["EXPECTED", "WAITING", "COMPLETED"])
        fun `PROCESSING 상태가 아니면 예외를 던진다`(currentStatus: InboundStatus) {
            val inbound: Inbound = inbound().inboundId(1L).status(currentStatus).build()

            assertThatThrownBy { inbound.complete() }
                .isInstanceOf(InvalidInboundStatusTransitionException::class.java)
                .hasMessage(InboundErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }
}
