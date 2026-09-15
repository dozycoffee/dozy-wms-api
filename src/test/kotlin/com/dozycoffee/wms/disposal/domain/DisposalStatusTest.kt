package com.dozycoffee.wms.disposal.domain

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DisposalStatusTest {

    @ParameterizedTest
    @CsvSource(
        "REQUESTED, 폐기 요청",
        "APPROVED, 폐기 승인",
        "COMPLETED, 폐기 완료"
    )
    fun `폐기 상태는 설명을 갖는다`(status: DisposalStatus, expectedDescription: String) {
        assertThat(status.description).isEqualTo(expectedDescription)
    }

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
