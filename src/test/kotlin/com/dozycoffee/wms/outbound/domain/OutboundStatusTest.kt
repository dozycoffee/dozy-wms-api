package com.dozycoffee.wms.outbound.domain

import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OutboundStatusTest {

    @ParameterizedTest
    @CsvSource(
        "REQUESTED, PICKING, true",
        "REQUESTED, INSPECTING, false",
        "REQUESTED, COMPLETED, false",
        "REQUESTED, REQUESTED, false",
        "PICKING, INSPECTING, true",
        "PICKING, REQUESTED, false",
        "PICKING, COMPLETED, false",
        "PICKING, PICKING, false",
        "INSPECTING, COMPLETED, true",
        "INSPECTING, REQUESTED, false",
        "INSPECTING, PICKING, false",
        "INSPECTING, INSPECTING, false",
        "COMPLETED, REQUESTED, false",
        "COMPLETED, PICKING, false",
        "COMPLETED, INSPECTING, false",
        "COMPLETED, COMPLETED, false"
    )
    fun `상태 전이 가능 여부를 판단한다`(current: OutboundStatus, target: OutboundStatus, expected: Boolean) {
        assertThat(current.canTransitionTo(target)).isEqualTo(expected)
    }
}
