package com.dozycoffee.wms.inventory.application.service

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
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ZoneInventorySummaryServiceTest {

    @Mock
    private lateinit var zoneInventorySummaryRepository: ZoneInventorySummaryRepository

    @InjectMocks
    private lateinit var zoneInventorySummaryService: ZoneInventorySummaryService

    @Test
    fun `Zone별 재고 현황을 리포지토리로부터 그대로 반환한다`() = runTest {
        val summary = ZoneInventorySummaryResult(1L, ZoneCode.A, 1L, 180, 90, mapOf(QualityStatus.NORMAL to 90))
        whenever(zoneInventorySummaryRepository.findAll(null)).thenReturn(flowOf(summary))

        val result = zoneInventorySummaryService.getAll(null).toList()

        assertThat(result).hasSize(1)
        assertThat(result[0].usageRate).isEqualTo(0.5)
    }

    @Test
    fun `warehouseIds가 주어지면 리포지토리에 그대로 전달한다`() = runTest {
        val summary = ZoneInventorySummaryResult(1L, ZoneCode.A, 1L, 180, 90, mapOf(QualityStatus.NORMAL to 90))
        whenever(zoneInventorySummaryRepository.findAll(listOf(1L, 2L))).thenReturn(flowOf(summary))

        val result = zoneInventorySummaryService.getAll(listOf(1L, 2L)).toList()

        assertThat(result).hasSize(1)
    }
}
