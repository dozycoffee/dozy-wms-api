package com.dozycoffee.wms.inbound.domain

import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class InboundStatusTest {

    @ParameterizedTest
    @CsvSource(
        "EXPECTED, 입고 예정",
        "WAITING, 입고 대기",
        "PROCESSING, 입고 처리중",
        "COMPLETED, 입고 완료"
    )
    fun `입고 상태는 설명을 갖는다`(status: InboundStatus, expectedDescription: String) {
        assertThat(status.description).isEqualTo(expectedDescription)
    }

    @ParameterizedTest
    @CsvSource(
        "EXPECTED, WAITING, true",
        "EXPECTED, PROCESSING, false",
        "EXPECTED, COMPLETED, false",
        "EXPECTED, EXPECTED, false",
        "WAITING, PROCESSING, true",
        "WAITING, EXPECTED, false",
        "WAITING, COMPLETED, false",
        "WAITING, WAITING, false",
        "PROCESSING, COMPLETED, true",
        "PROCESSING, EXPECTED, false",
        "PROCESSING, WAITING, false",
        "PROCESSING, PROCESSING, false",
        "COMPLETED, EXPECTED, false",
        "COMPLETED, WAITING, false",
        "COMPLETED, PROCESSING, false",
        "COMPLETED, COMPLETED, false"
    )
    fun `상태 전이 가능 여부를 판단한다`(current: InboundStatus, target: InboundStatus, expected: Boolean) {
        assertThat(current.canTransitionTo(target)).isEqualTo(expected)
    }
}
