package com.dozycoffee.wms.product.domain

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ProductCategoryTest {

    @ParameterizedTest
    @CsvSource(
        "BEAN, A",
        "SYRUP, B",
        "POWDER, C",
        "DAIRY, D",
        "SUPPLY, E",
        "MD, F"
    )
    fun `카테고리는 창고 Zone 코드와 1대1로 매핑된다`(category: ProductCategory, expectedZoneCode: ZoneCode) {
        assertThat(category.zoneCode).isEqualTo(expectedZoneCode)
    }
}
