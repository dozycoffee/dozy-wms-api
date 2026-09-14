package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AllocationStatusTest {

    @ParameterizedTest
    @CsvSource(
        "AVAILABLE, 가용",
        "ALLOCATED, 할당됨"
    )
    fun `할당 상태는 설명을 갖는다`(allocationStatus: AllocationStatus, expectedDescription: String) {
        assertThat(allocationStatus.description).isEqualTo(expectedDescription)
    }
}
