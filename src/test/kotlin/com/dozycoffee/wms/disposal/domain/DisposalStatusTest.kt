package com.dozycoffee.wms.disposal.domain

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DisposalStatusTest {

    @ParameterizedTest
    @CsvSource(
        "REQUESTED, APPROVED, true",
        "REQUESTED, COMPLETED, false",
        "REQUESTED, REQUESTED, false",
        "APPROVED, COMPLETED, true",
        "APPROVED, REQUESTED, false",
        "APPROVED, APPROVED, false",
        "COMPLETED, REQUESTED, false",
        "COMPLETED, APPROVED, false",
        "COMPLETED, COMPLETED, false"
    )
    fun `상태 전이 가능 여부를 판단한다`(current: DisposalStatus, target: DisposalStatus, expected: Boolean) {
        assertThat(current.canTransitionTo(target)).isEqualTo(expected)
    }
}
