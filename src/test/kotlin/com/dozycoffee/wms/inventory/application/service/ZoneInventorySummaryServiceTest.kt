package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.global.security.AccessScope
import com.dozycoffee.wms.global.security.CurrentAccessScopeProvider
import com.dozycoffee.wms.inventory.application.port.`in`.result.ZoneInventorySummaryResult
import com.dozycoffee.wms.inventory.application.port.out.ZoneInventorySummaryRepository
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ZoneInventorySummaryServiceTest {

    @Mock
    private lateinit var zoneInventorySummaryRepository: ZoneInventorySummaryRepository

    @Mock
    private lateinit var currentAccessScopeProvider: CurrentAccessScopeProvider

    @InjectMocks
    private lateinit var zoneInventorySummaryService: ZoneInventorySummaryService

    @Test
    fun `Zone별 재고 현황을 리포지토리로부터 그대로 반환한다`() = runTest {
        val summary = ZoneInventorySummaryResult(1L, ZoneCode.A, 1L, 180, 90, mapOf(QualityStatus.NORMAL to 90))
        whenever(currentAccessScopeProvider.get()).thenReturn(AccessScope(userId = "tester"))
        whenever(zoneInventorySummaryRepository.findAll(null)).thenReturn(flowOf(summary))

        val result = zoneInventorySummaryService.getAll(null).toList()

        assertThat(result).hasSize(1)
        assertThat(result[0].usageRate).isEqualTo(0.5)
    }

    @Test
    fun `스코프에 제한이 없으면 요청한 warehouseIds를 그대로 리포지토리에 전달한다`() = runTest {
        val summary = ZoneInventorySummaryResult(1L, ZoneCode.A, 1L, 180, 90, mapOf(QualityStatus.NORMAL to 90))
        whenever(currentAccessScopeProvider.get()).thenReturn(AccessScope(userId = "tester"))
        whenever(zoneInventorySummaryRepository.findAll(listOf(1L, 2L))).thenReturn(flowOf(summary))

        val result = zoneInventorySummaryService.getAll(listOf(1L, 2L)).toList()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `스코프가 제한적이면 요청한 warehouseIds와의 교집합만 리포지토리에 전달한다`() = runTest {
        val summary = ZoneInventorySummaryResult(1L, ZoneCode.A, 1L, 180, 90, mapOf(QualityStatus.NORMAL to 90))
        whenever(currentAccessScopeProvider.get()).thenReturn(AccessScope(userId = "tester", warehouseIds = listOf(2L, 3L)))
        whenever(zoneInventorySummaryRepository.findAll(listOf(2L))).thenReturn(flowOf(summary))

        val result = zoneInventorySummaryService.getAll(listOf(1L, 2L)).toList()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `요청한 warehouseIds가 스코프와 전혀 겹치지 않으면 리포지토리를 조회하지 않고 빈 목록을 반환한다`() = runTest {
        whenever(currentAccessScopeProvider.get()).thenReturn(AccessScope(userId = "tester", warehouseIds = listOf(2L)))

        val result = zoneInventorySummaryService.getAll(listOf(1L)).toList()

        assertThat(result).isEmpty()
        verify(zoneInventorySummaryRepository, never()).findAll(any())
    }
}
