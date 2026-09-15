package com.dozycoffee.wms.outbound.domain

import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OutboundStatusTest {

    @ParameterizedTest
    @CsvSource(
        "REQUESTED, 출고 요청",
        "PICKING, 피킹중",
        "INSPECTING, 검수중",
        "COMPLETED, 출고 완료"
    )
    fun `출고 상태는 설명을 갖는다`(status: OutboundStatus, expectedDescription: String) {
        assertThat(status.description).isEqualTo(expectedDescription)
    }

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
