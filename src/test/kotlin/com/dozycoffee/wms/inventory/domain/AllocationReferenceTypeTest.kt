package com.dozycoffee.wms.inventory.domain

import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AllocationReferenceTypeTest {

    @ParameterizedTest
    @CsvSource(
        "OUTBOUND, 출고"
    )
    fun `참조 유형은 설명을 갖는다`(referenceType: AllocationReferenceType, expectedDescription: String) {
        assertThat(referenceType.description).isEqualTo(expectedDescription)
    }
}
