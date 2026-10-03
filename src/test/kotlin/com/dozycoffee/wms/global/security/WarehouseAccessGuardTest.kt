package com.dozycoffee.wms.global.security

import com.dozycoffee.wms.global.error.ErrorType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class WarehouseAccessGuardTest {

    private fun guardOf(access: WarehouseAccess): WarehouseAccessGuard =
        WarehouseAccessGuard(object : CurrentWarehouseAccessProvider {
            override suspend fun current(): WarehouseAccess = access
        })

    @Test
    fun `접근 가능한 창고면 통과한다`() = runTest {
        assertThatCode { runBlocking { guardOf(OnlyWarehouses(setOf(1L))).require(1L) } }
            .doesNotThrowAnyException()
    }

    @Test
    fun `접근할 수 없는 창고면 FORBIDDEN 예외를 던진다`() = runTest {
        assertThatThrownBy { runBlocking { guardOf(OnlyWarehouses(setOf(1L))).require(2L) } }
            .isInstanceOfSatisfying(WarehouseAccessDeniedException::class.java) {
                assertThat(it.errorCode.errorType).isEqualTo(ErrorType.FORBIDDEN)
            }
    }

    @Test
    fun `접근 가능한 창고가 없으면 어떤 창고도 통과하지 못한다`() = runTest {
        assertThatThrownBy { runBlocking { guardOf(OnlyWarehouses(emptySet())).require(1L) } }
            .isInstanceOf(WarehouseAccessDeniedException::class.java)
    }

    @Test
    fun `narrow는 접근 범위로 좁힌 조건을 반환한다`() = runTest {
        assertThat(guardOf(OnlyWarehouses(setOf(1L))).narrow(listOf(1L, 2L))).isEqualTo(WarehouseFilter.In(listOf(1L)))
        assertThat(guardOf(AllWarehouses).narrow(null)).isEqualTo(WarehouseFilter.Unfiltered)
    }
}
