package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class QualityStatusTest {

    @ParameterizedTest
    @CsvSource(
        "NORMAL, 정상",
        "DEFECTIVE, 불량",
        "DISPOSAL_SCHEDULED, 폐기 예정"
    )
    fun `품질 상태는 설명을 갖는다`(qualityStatus: QualityStatus, expectedDescription: String) {
        assertThat(qualityStatus.description).isEqualTo(expectedDescription)
    }
}
