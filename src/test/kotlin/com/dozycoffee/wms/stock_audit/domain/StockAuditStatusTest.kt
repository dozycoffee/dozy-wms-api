package com.dozycoffee.wms.stock_audit.domain

import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class StockAuditStatusTest {

    @ParameterizedTest
    @CsvSource(
        "SCHEDULED, IN_PROGRESS, true",
        "SCHEDULED, COMPLETED, false",
        "SCHEDULED, CLOSED, false",
        "SCHEDULED, SCHEDULED, false",
        "IN_PROGRESS, COMPLETED, true",
        "IN_PROGRESS, SCHEDULED, false",
        "IN_PROGRESS, CLOSED, false",
        "IN_PROGRESS, IN_PROGRESS, false",
        "COMPLETED, CLOSED, true",
        "COMPLETED, SCHEDULED, false",
        "COMPLETED, IN_PROGRESS, false",
        "COMPLETED, COMPLETED, false",
        "CLOSED, SCHEDULED, false",
        "CLOSED, IN_PROGRESS, false",
        "CLOSED, COMPLETED, false",
        "CLOSED, CLOSED, false"
    )
    fun `상태 전이 가능 여부를 판단한다`(current: StockAuditStatus, target: StockAuditStatus, expected: Boolean) {
        assertThat(current.canTransitionTo(target)).isEqualTo(expected)
    }
}
