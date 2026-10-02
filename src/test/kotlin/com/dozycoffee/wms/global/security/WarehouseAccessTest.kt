package com.dozycoffee.wms.global.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WarehouseAccessTest {

    @Nested
    inner class 전체_창고_접근 {

        @Test
        fun `요청값이 없거나 비어 있으면 조건 없이 조회한다`() {
            assertThat(AllWarehouses.narrow(null)).isEqualTo(WarehouseFilter.Unfiltered)
            assertThat(AllWarehouses.narrow(emptyList())).isEqualTo(WarehouseFilter.Unfiltered)
        }

        @Test
        fun `요청한 창고 ID를 그대로 조건으로 반환한다`() {
            assertThat(AllWarehouses.narrow(listOf(1L, 2L))).isEqualTo(WarehouseFilter.In(listOf(1L, 2L)))
        }

        @Test
        fun `모든 창고에 접근할 수 있다`() {
            assertThat(AllWarehouses.canAccess(999L)).isTrue()
        }
    }

    @Nested
    inner class 일부_창고_접근 {

        private val access = OnlyWarehouses(setOf(1L, 2L))

        @Test
        fun `요청값이 없으면 접근 가능한 창고 전체를 조건으로 반환한다`() {
            val filter = access.narrow(null)

            assertThat(filter).isInstanceOf(WarehouseFilter.In::class.java)
            assertThat((filter as WarehouseFilter.In).warehouseIds).containsExactlyInAnyOrder(1L, 2L)
        }

        @Test
        fun `요청값과 접근 범위의 교집합만 조건으로 반환한다`() {
            assertThat(access.narrow(listOf(2L, 3L))).isEqualTo(WarehouseFilter.In(listOf(2L)))
        }

        @Test
        fun `요청값이 접근 범위와 겹치지 않으면 결과 없음이다`() {
            assertThat(access.narrow(listOf(3L))).isEqualTo(WarehouseFilter.None)
        }

        @Test
        fun `접근 가능한 창고가 하나도 없으면 요청과 무관하게 결과 없음이다`() {
            val none = OnlyWarehouses(emptySet())

            assertThat(none.narrow(null)).isEqualTo(WarehouseFilter.None)
            assertThat(none.narrow(listOf(1L))).isEqualTo(WarehouseFilter.None)
        }

        @Test
        fun `접근 범위에 포함된 창고만 접근할 수 있다`() {
            assertThat(access.canAccess(1L)).isTrue()
            assertThat(access.canAccess(3L)).isFalse()
        }

        @Test
        fun `접근 가능한 창고가 없으면 어떤 창고에도 접근할 수 없다`() {
            assertThat(OnlyWarehouses(emptySet()).canAccess(1L)).isFalse()
        }
    }
}
