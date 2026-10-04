package com.dozycoffee.wms.inbound.domain

import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class InboundStatusTest {

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
