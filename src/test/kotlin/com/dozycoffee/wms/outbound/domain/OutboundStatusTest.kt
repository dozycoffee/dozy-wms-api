package com.dozycoffee.wms.outbound.domain

import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OutboundStatusTest {

    @ParameterizedTest
    @CsvSource(
        "REQUESTED, REQUESTED, false",
        "REQUESTED, PICKING, true",
        "REQUESTED, INSPECTING, false",
        "REQUESTED, COMPLETED, false",
        "REQUESTED, CANCEL_REQUESTED, false",
        "REQUESTED, CANCELED, true",
        "PICKING, REQUESTED, false",
        "PICKING, PICKING, false",
        "PICKING, INSPECTING, true",
        "PICKING, COMPLETED, false",
        "PICKING, CANCEL_REQUESTED, true",
        "PICKING, CANCELED, false",
        "INSPECTING, REQUESTED, false",
        "INSPECTING, PICKING, false",
        "INSPECTING, INSPECTING, false",
        "INSPECTING, COMPLETED, true",
        "INSPECTING, CANCEL_REQUESTED, true",
        "INSPECTING, CANCELED, false",
        "COMPLETED, REQUESTED, false",
        "COMPLETED, PICKING, false",
        "COMPLETED, INSPECTING, false",
        "COMPLETED, COMPLETED, false",
        "COMPLETED, CANCEL_REQUESTED, false",
        "COMPLETED, CANCELED, false",
        "CANCEL_REQUESTED, REQUESTED, false",
        "CANCEL_REQUESTED, PICKING, false",
        "CANCEL_REQUESTED, INSPECTING, false",
        "CANCEL_REQUESTED, COMPLETED, false",
        "CANCEL_REQUESTED, CANCEL_REQUESTED, false",
        "CANCEL_REQUESTED, CANCELED, true",
        "CANCELED, REQUESTED, false",
        "CANCELED, PICKING, false",
        "CANCELED, INSPECTING, false",
        "CANCELED, COMPLETED, false",
        "CANCELED, CANCEL_REQUESTED, false",
        "CANCELED, CANCELED, false"
    )
    fun `상태 전이 가능 여부를 판단한다`(current: OutboundStatus, target: OutboundStatus, expected: Boolean) {
        assertThat(current.canTransitionTo(target)).isEqualTo(expected)
    }
}
