package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LotStatusTest {

    @ParameterizedTest
    @CsvSource(
        "NORMAL, EXPIRING_SOON, true",
        "NORMAL, EXPIRED, true",
        "NORMAL, NORMAL, false",
        "EXPIRING_SOON, EXPIRED, true",
        "EXPIRING_SOON, NORMAL, false",
        "EXPIRING_SOON, EXPIRING_SOON, false",
        "EXPIRED, NORMAL, false",
        "EXPIRED, EXPIRING_SOON, false",
        "EXPIRED, EXPIRED, false"
    )
    fun `상태 전이 가능 여부를 판단한다`(current: LotStatus, target: LotStatus, expected: Boolean) {
        assertThat(current.canTransitionTo(target)).isEqualTo(expected)
    }
}
