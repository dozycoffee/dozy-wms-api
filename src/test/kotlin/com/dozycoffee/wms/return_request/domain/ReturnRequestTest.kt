package com.dozycoffee.wms.return_request.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.exception.InvalidReturnRequestStatusTransitionException
import com.dozycoffee.wms.return_request.domain.exception.ReturnRequestErrorCode
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import com.dozycoffee.wms.return_request.fixture.ReturnRequestTestBuilder.Companion.returnRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ReturnRequestTest {

    @Nested
    inner class 반품_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 RECEIVED 상태로 생성된다`() {
            val request: ReturnRequest = returnRequest().build()

            assertThat(request.warehouseId).isEqualTo(1L)
            assertThat(request.status).isEqualTo(ReturnRequestStatus.RECEIVED)
        }

        @Test
        fun `창고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { returnRequest().warehouseId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ReturnRequestErrorCode.INVALID_WAREHOUSE_ID.message)
        }
    }

    @Nested
    inner class 반품_재구성 {

        @Test
        fun `저장된 ID와 상태로 반품 객체를 재구성한다`() {
            val request: ReturnRequest =
                returnRequest().returnRequestId(100L).status(ReturnRequestStatus.INSPECTING).build()

            assertThat(request.returnRequestId).isEqualTo(100L)
            assertThat(request.status).isEqualTo(ReturnRequestStatus.INSPECTING)
        }
    }

    @Nested
    inner class 검수_시작 {

        @Test
        fun `RECEIVED 상태는 INSPECTING으로 전환된다`() {
            val request: ReturnRequest =
                returnRequest().returnRequestId(1L).status(ReturnRequestStatus.RECEIVED).build()

            request.startInspecting()

            assertThat(request.status).isEqualTo(ReturnRequestStatus.INSPECTING)
        }

        @ParameterizedTest
        @EnumSource(value = ReturnRequestStatus::class, names = ["INSPECTING", "COMPLETED"])
        fun `RECEIVED 상태가 아니면 예외를 던진다`(currentStatus: ReturnRequestStatus) {
            val request: ReturnRequest = returnRequest().returnRequestId(1L).status(currentStatus).build()

            assertThatThrownBy { request.startInspecting() }
                .isInstanceOf(InvalidReturnRequestStatusTransitionException::class.java)
                .hasMessage(ReturnRequestErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 반품_완료 {

        @Test
        fun `INSPECTING 상태는 COMPLETED로 전환된다`() {
            val request: ReturnRequest =
                returnRequest().returnRequestId(1L).status(ReturnRequestStatus.INSPECTING).build()

            request.complete()

            assertThat(request.status).isEqualTo(ReturnRequestStatus.COMPLETED)
        }

        @ParameterizedTest
        @EnumSource(value = ReturnRequestStatus::class, names = ["RECEIVED", "COMPLETED"])
        fun `INSPECTING 상태가 아니면 예외를 던진다`(currentStatus: ReturnRequestStatus) {
            val request: ReturnRequest = returnRequest().returnRequestId(1L).status(currentStatus).build()

            assertThatThrownBy { request.complete() }
                .isInstanceOf(InvalidReturnRequestStatusTransitionException::class.java)
                .hasMessage(ReturnRequestErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }
}
