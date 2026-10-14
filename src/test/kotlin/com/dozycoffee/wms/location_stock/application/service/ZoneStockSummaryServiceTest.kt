package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.security.AllWarehouses
import com.dozycoffee.wms.global.security.CurrentWarehouseAccessProvider
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.location_stock.application.port.`in`.result.ZoneStockSummaryResult
import com.dozycoffee.wms.location_stock.application.port.out.ZoneStockSummaryRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
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
class ZoneStockSummaryServiceTest {

    @Mock
    private lateinit var zoneStockSummaryRepository: ZoneStockSummaryRepository

    @Mock
    private lateinit var currentWarehouseAccessProvider: CurrentWarehouseAccessProvider

    @InjectMocks
    private lateinit var zoneStockSummaryService: ZoneStockSummaryService

    @Test
    fun `Zone별 재고 현황을 리포지토리로부터 그대로 반환한다`() = runTest {
        val summary = ZoneStockSummaryResult(1L, ZoneCode.A, 1L, 180, 90, mapOf(StockQualityStatus.NORMAL to 90))
        whenever(currentWarehouseAccessProvider.current()).thenReturn(AllWarehouses)
        whenever(zoneStockSummaryRepository.findAll(null)).thenReturn(flowOf(summary))

        val result = zoneStockSummaryService.getAll(null).toList()

        assertThat(result).hasSize(1)
        assertThat(result[0].usageRate).isEqualTo(0.5)
    }

    @Test
    fun `접근 범위가 전체면 요청한 warehouseIds를 그대로 리포지토리에 전달한다`() = runTest {
        val summary = ZoneStockSummaryResult(1L, ZoneCode.A, 1L, 180, 90, mapOf(StockQualityStatus.NORMAL to 90))
        whenever(currentWarehouseAccessProvider.current()).thenReturn(AllWarehouses)
        whenever(zoneStockSummaryRepository.findAll(listOf(1L, 2L))).thenReturn(flowOf(summary))

        val result = zoneStockSummaryService.getAll(listOf(1L, 2L)).toList()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `접근 범위가 제한적이면 요청한 warehouseIds와의 교집합만 리포지토리에 전달한다`() = runTest {
        val summary = ZoneStockSummaryResult(1L, ZoneCode.A, 1L, 180, 90, mapOf(StockQualityStatus.NORMAL to 90))
        whenever(currentWarehouseAccessProvider.current()).thenReturn(OnlyWarehouses(setOf(2L, 3L)))
        whenever(zoneStockSummaryRepository.findAll(listOf(2L))).thenReturn(flowOf(summary))

        val result = zoneStockSummaryService.getAll(listOf(1L, 2L)).toList()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `요청한 warehouseIds가 접근 범위와 전혀 겹치지 않으면 리포지토리를 조회하지 않고 빈 목록을 반환한다`() = runTest {
        whenever(currentWarehouseAccessProvider.current()).thenReturn(OnlyWarehouses(setOf(2L)))

        val result = zoneStockSummaryService.getAll(listOf(1L)).toList()

        assertThat(result).isEmpty()
        verify(zoneStockSummaryRepository, never()).findAll(any())
    }
}
