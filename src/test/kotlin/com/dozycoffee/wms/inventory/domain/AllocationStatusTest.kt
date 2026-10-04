package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AllocationStatusTest {

    @ParameterizedTest
    @CsvSource(
        "HELD, RELEASED, true",
        "HELD, FULFILLED, true",
        "HELD, HELD, false",
        "RELEASED, HELD, false",
        "RELEASED, FULFILLED, false",
        "RELEASED, RELEASED, false",
        "FULFILLED, HELD, false",
        "FULFILLED, RELEASED, false",
        "FULFILLED, FULFILLED, false"
    )
    fun `상태 전이 가능 여부를 판단한다`(current: AllocationStatus, target: AllocationStatus, expected: Boolean) {
        assertThat(current.canTransitionTo(target)).isEqualTo(expected)
    }
}
