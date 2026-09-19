package com.dozycoffee.wms.global.security

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MockAccessScopeProviderTest {

    private val mockAccessScopeProvider = MockAccessScopeProvider()

    @Test
    fun `get()은 고정된 userId와 빈 warehouseIds를 가진 AccessScope를 반환한다`() = runTest {
        val accessScope = mockAccessScopeProvider.get()

        assertThat(accessScope.userId).isEqualTo("system")
        assertThat(accessScope.warehouseIds).isEmpty()
    }
}
