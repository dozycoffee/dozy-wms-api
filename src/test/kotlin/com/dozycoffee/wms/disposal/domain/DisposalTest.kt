package com.dozycoffee.wms.disposal.domain

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.exception.DisposalErrorCode
import com.dozycoffee.wms.disposal.domain.exception.InvalidDisposalStatusTransitionException
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.disposal.fixture.DisposalTestBuilder.Companion.disposal
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DisposalTest {

    @Nested
    inner class 폐기_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 REQUESTED 상태로 생성된다`() {
            val disposal: Disposal = disposal().build()

            assertThat(disposal.warehouseId).isEqualTo(1L)
            assertThat(disposal.status).isEqualTo(DisposalStatus.REQUESTED)
        }

        @Test
        fun `창고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { disposal().warehouseId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(DisposalErrorCode.INVALID_WAREHOUSE_ID.message)
        }
    }

    @Nested
    inner class 폐기_재구성 {

        @Test
        fun `저장된 ID와 상태로 폐기 객체를 재구성한다`() {
            val disposal: Disposal = disposal().disposalId(100L).status(DisposalStatus.APPROVED).build()

            assertThat(disposal.disposalId).isEqualTo(100L)
            assertThat(disposal.status).isEqualTo(DisposalStatus.APPROVED)
        }
    }

    @Nested
    inner class 폐기_승인 {

        @Test
        fun `REQUESTED 상태는 APPROVED로 전환된다`() {
            val disposal: Disposal = disposal().disposalId(1L).status(DisposalStatus.REQUESTED).build()

            disposal.approve()

            assertThat(disposal.status).isEqualTo(DisposalStatus.APPROVED)
        }

        @ParameterizedTest
        @EnumSource(value = DisposalStatus::class, names = ["APPROVED", "COMPLETED"])
        fun `REQUESTED 상태가 아니면 예외를 던진다`(currentStatus: DisposalStatus) {
            val disposal: Disposal = disposal().disposalId(1L).status(currentStatus).build()

            assertThatThrownBy { disposal.approve() }
                .isInstanceOf(InvalidDisposalStatusTransitionException::class.java)
                .hasMessage(DisposalErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 폐기_완료 {

        @Test
        fun `APPROVED 상태는 COMPLETED로 전환된다`() {
            val disposal: Disposal = disposal().disposalId(1L).status(DisposalStatus.APPROVED).build()

            disposal.complete()

            assertThat(disposal.status).isEqualTo(DisposalStatus.COMPLETED)
        }

        @ParameterizedTest
        @EnumSource(value = DisposalStatus::class, names = ["REQUESTED", "COMPLETED"])
        fun `APPROVED 상태가 아니면 예외를 던진다`(currentStatus: DisposalStatus) {
            val disposal: Disposal = disposal().disposalId(1L).status(currentStatus).build()

            assertThatThrownBy { disposal.complete() }
                .isInstanceOf(InvalidDisposalStatusTransitionException::class.java)
                .hasMessage(DisposalErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }
}
