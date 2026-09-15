package com.dozycoffee.wms.outbound.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.InvalidOutboundStatusTransitionException
import com.dozycoffee.wms.outbound.domain.exception.OutboundErrorCode
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class OutboundTest {

    @Nested
    inner class 출고_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 REQUESTED 상태로 생성된다`() {
            val outbound: Outbound = outbound().build()

            assertThat(outbound.warehouseId).isEqualTo(1L)
            assertThat(outbound.status).isEqualTo(OutboundStatus.REQUESTED)
        }

        @Test
        fun `창고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { outbound().warehouseId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(OutboundErrorCode.INVALID_WAREHOUSE_ID.message)
        }
    }

    @Nested
    inner class 출고_재구성 {

        @Test
        fun `저장된 ID와 상태로 출고 객체를 재구성한다`() {
            val outbound: Outbound = outbound().outboundId(100L).status(OutboundStatus.PICKING).build()

            assertThat(outbound.outboundId).isEqualTo(100L)
            assertThat(outbound.status).isEqualTo(OutboundStatus.PICKING)
        }
    }

    @Nested
    inner class 피킹_시작 {

        @Test
        fun `REQUESTED 상태는 PICKING으로 전환된다`() {
            val outbound: Outbound = outbound().outboundId(1L).status(OutboundStatus.REQUESTED).build()

            outbound.startPicking()

            assertThat(outbound.status).isEqualTo(OutboundStatus.PICKING)
        }

        @ParameterizedTest
        @EnumSource(value = OutboundStatus::class, names = ["PICKING", "INSPECTING", "COMPLETED"])
        fun `REQUESTED 상태가 아니면 예외를 던진다`(currentStatus: OutboundStatus) {
            val outbound: Outbound = outbound().outboundId(1L).status(currentStatus).build()

            assertThatThrownBy { outbound.startPicking() }
                .isInstanceOf(InvalidOutboundStatusTransitionException::class.java)
                .hasMessage(OutboundErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 검수_시작 {

        @Test
        fun `PICKING 상태는 INSPECTING으로 전환된다`() {
            val outbound: Outbound = outbound().outboundId(1L).status(OutboundStatus.PICKING).build()

            outbound.startInspecting()

            assertThat(outbound.status).isEqualTo(OutboundStatus.INSPECTING)
        }

        @ParameterizedTest
        @EnumSource(value = OutboundStatus::class, names = ["REQUESTED", "INSPECTING", "COMPLETED"])
        fun `PICKING 상태가 아니면 예외를 던진다`(currentStatus: OutboundStatus) {
            val outbound: Outbound = outbound().outboundId(1L).status(currentStatus).build()

            assertThatThrownBy { outbound.startInspecting() }
                .isInstanceOf(InvalidOutboundStatusTransitionException::class.java)
                .hasMessage(OutboundErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 출고_완료 {

        @Test
        fun `INSPECTING 상태는 COMPLETED로 전환된다`() {
            val outbound: Outbound = outbound().outboundId(1L).status(OutboundStatus.INSPECTING).build()

            outbound.complete()

            assertThat(outbound.status).isEqualTo(OutboundStatus.COMPLETED)
        }

        @ParameterizedTest
        @EnumSource(value = OutboundStatus::class, names = ["REQUESTED", "PICKING", "COMPLETED"])
        fun `INSPECTING 상태가 아니면 예외를 던진다`(currentStatus: OutboundStatus) {
            val outbound: Outbound = outbound().outboundId(1L).status(currentStatus).build()

            assertThatThrownBy { outbound.complete() }
                .isInstanceOf(InvalidOutboundStatusTransitionException::class.java)
                .hasMessage(OutboundErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }
}
