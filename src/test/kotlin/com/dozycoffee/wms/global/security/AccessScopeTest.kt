package com.dozycoffee.wms.global.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AccessScopeTest {

    @Nested
    inner class warehouseIds_좁히기 {

        @Test
        fun `스코프의 warehouseIds가 비어있으면(무제한) 요청값을 그대로 반환한다`() {
            val accessScope = AccessScope(userId = "tester", warehouseIds = emptyList())

            assertThat(accessScope.narrowWarehouseIds(null)).isNull()
            assertThat(accessScope.narrowWarehouseIds(listOf(1L, 2L))).containsExactly(1L, 2L)
        }

        @Test
        fun `스코프가 제한적이고 요청값이 없으면 스코프 전체를 반환한다`() {
            val accessScope = AccessScope(userId = "tester", warehouseIds = listOf(1L, 2L))

            val result = accessScope.narrowWarehouseIds(null)

            assertThat(result).containsExactly(1L, 2L)
        }

        @Test
        fun `스코프가 제한적이면 요청값과의 교집합만 반환한다`() {
            val accessScope = AccessScope(userId = "tester", warehouseIds = listOf(1L, 2L))

            val result = accessScope.narrowWarehouseIds(listOf(2L, 3L))

            assertThat(result).containsExactly(2L)
        }

        @Test
        fun `요청값이 스코프와 전혀 겹치지 않으면 빈 리스트를 반환한다`() {
            val accessScope = AccessScope(userId = "tester", warehouseIds = listOf(1L, 2L))

            val result = accessScope.narrowWarehouseIds(listOf(3L))

            assertThat(result).isEmpty()
        }
    }
}
