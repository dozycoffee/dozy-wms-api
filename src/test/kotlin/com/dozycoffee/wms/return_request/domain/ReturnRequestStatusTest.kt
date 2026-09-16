package com.dozycoffee.wms.return_request.domain

import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ReturnRequestStatusTest {

    @ParameterizedTest
    @CsvSource(
        "RECEIVED, 반품 접수",
        "INSPECTING, 검수 중",
        "COMPLETED, 반품 완료"
    )
    fun `반품 상태는 설명을 갖는다`(status: ReturnRequestStatus, expectedDescription: String) {
        assertThat(status.description).isEqualTo(expectedDescription)
    }

    @ParameterizedTest
    @CsvSource(
        "RECEIVED, INSPECTING, true",
        "RECEIVED, COMPLETED, false",
        "RECEIVED, RECEIVED, false",
        "INSPECTING, COMPLETED, true",
        "INSPECTING, RECEIVED, false",
        "INSPECTING, INSPECTING, false",
        "COMPLETED, RECEIVED, false",
        "COMPLETED, INSPECTING, false",
        "COMPLETED, COMPLETED, false"
    )
    fun `상태 전이 가능 여부를 판단한다`(current: ReturnRequestStatus, target: ReturnRequestStatus, expected: Boolean) {
        assertThat(current.canTransitionTo(target)).isEqualTo(expected)
    }
}
