package com.dozycoffee.wms.stock_audit.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.exception.InvalidStockAuditStatusTransitionException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditApprovalRequiredException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditErrorCode
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import com.dozycoffee.wms.stock_audit.fixture.StockAuditTestBuilder.Companion.stockAudit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class StockAuditTest {

    @Nested
    inner class 실사_등록 {

        @Test
        fun `정상적인 정보를 입력했을 때 SCHEDULED 상태로 생성된다`() {
            val audit: StockAudit = stockAudit().build()

            assertThat(audit.warehouseId).isEqualTo(1L)
            assertThat(audit.zoneId).isEqualTo(1L)
            assertThat(audit.status).isEqualTo(StockAuditStatus.SCHEDULED)
            assertThat(audit.assignee).isNull()
        }

        @Test
        fun `창고 ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { stockAudit().warehouseId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(StockAuditErrorCode.INVALID_WAREHOUSE_ID.message)
        }

        @Test
        fun `Zone ID가 null이면 예외를 던진다`() {
            assertThatThrownBy { stockAudit().zoneId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(StockAuditErrorCode.INVALID_ZONE_ID.message)
        }
    }

    @Nested
    inner class 실사_재구성 {

        @Test
        fun `저장된 ID와 상태로 실사 객체를 재구성한다`() {
            val audit: StockAudit = stockAudit().stockAuditId(100L).status(StockAuditStatus.IN_PROGRESS).build()

            assertThat(audit.stockAuditId).isEqualTo(100L)
            assertThat(audit.status).isEqualTo(StockAuditStatus.IN_PROGRESS)
        }
    }

    @Nested
    inner class 담당자_배정 {

        @Test
        fun `SCHEDULED 상태에서 담당자를 배정하면 IN_PROGRESS로 전환된다`() {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.SCHEDULED).build()

            audit.assign("담당자A")

            assertThat(audit.status).isEqualTo(StockAuditStatus.IN_PROGRESS)
            assertThat(audit.assignee).isEqualTo("담당자A")
        }

        @Test
        fun `담당자가 공백이면 예외를 던진다`() {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.SCHEDULED).build()

            assertThatThrownBy { audit.assign(" ") }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(StockAuditErrorCode.INVALID_ASSIGNEE.message)
        }

        @ParameterizedTest
        @EnumSource(value = StockAuditStatus::class, names = ["IN_PROGRESS", "COMPLETED", "CLOSED"])
        fun `SCHEDULED 상태가 아니면 예외를 던진다`(currentStatus: StockAuditStatus) {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(currentStatus).build()

            assertThatThrownBy { audit.assign("담당자A") }
                .isInstanceOf(InvalidStockAuditStatusTransitionException::class.java)
                .hasMessage(StockAuditErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 실사_완료 {

        @Test
        fun `IN_PROGRESS 상태는 COMPLETED로 전환된다`() {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.IN_PROGRESS).build()

            audit.complete()

            assertThat(audit.status).isEqualTo(StockAuditStatus.COMPLETED)
        }

        @ParameterizedTest
        @EnumSource(value = StockAuditStatus::class, names = ["SCHEDULED", "COMPLETED", "CLOSED"])
        fun `IN_PROGRESS 상태가 아니면 예외를 던진다`(currentStatus: StockAuditStatus) {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(currentStatus).build()

            assertThatThrownBy { audit.complete() }
                .isInstanceOf(InvalidStockAuditStatusTransitionException::class.java)
                .hasMessage(StockAuditErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }

    @Nested
    inner class 조정_마감 {

        @Test
        fun `승인이 필요 없으면 COMPLETED 상태에서 바로 CLOSED로 전환된다`() {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()

            audit.close(requiresApproval = false, approvedBy = null)

            assertThat(audit.status).isEqualTo(StockAuditStatus.CLOSED)
            assertThat(audit.approvedBy).isNull()
        }

        @Test
        fun `승인이 필요하고 승인자가 있으면 CLOSED로 전환된다`() {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()

            audit.close(requiresApproval = true, approvedBy = "관리자A")

            assertThat(audit.status).isEqualTo(StockAuditStatus.CLOSED)
            assertThat(audit.approvedBy).isEqualTo("관리자A")
        }

        @Test
        fun `승인이 필요한데 승인자가 없으면 예외를 던진다`() {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()

            assertThatThrownBy { audit.close(requiresApproval = true, approvedBy = null) }
                .isInstanceOf(StockAuditApprovalRequiredException::class.java)
                .hasMessage(StockAuditErrorCode.APPROVAL_REQUIRED.message)
        }

        @ParameterizedTest
        @EnumSource(value = StockAuditStatus::class, names = ["SCHEDULED", "IN_PROGRESS", "CLOSED"])
        fun `COMPLETED 상태가 아니면 예외를 던진다`(currentStatus: StockAuditStatus) {
            val audit: StockAudit = stockAudit().stockAuditId(1L).status(currentStatus).build()

            assertThatThrownBy { audit.close(requiresApproval = false, approvedBy = null) }
                .isInstanceOf(InvalidStockAuditStatusTransitionException::class.java)
                .hasMessage(StockAuditErrorCode.INVALID_STATUS_TRANSITION.message)
        }
    }
}
