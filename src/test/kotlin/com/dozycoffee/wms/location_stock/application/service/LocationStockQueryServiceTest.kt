package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.security.AllWarehouses
import com.dozycoffee.wms.global.security.CurrentWarehouseAccessProvider
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.location_stock.application.port.`in`.query.LocationStockQuery
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockDetailResult
import com.dozycoffee.wms.location_stock.application.port.out.LocationStockQueryRepository
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
class LocationStockQueryServiceTest {

    @Mock
    private lateinit var locationStockQueryRepository: LocationStockQueryRepository

    @Mock
    private lateinit var currentWarehouseAccessProvider: CurrentWarehouseAccessProvider

    @InjectMocks
    private lateinit var locationStockQueryService: LocationStockQueryService

    private fun detail() = LocationStockDetailResult(
        1L, 10L, 5L, ZoneCode.A, 7L, "A-01", 100L, "LOT-1", StockQualityStatus.NORMAL, 30, 10, null
    )

    @Test
    fun `가용 수량은 총 수량에서 점유 수량을 뺀 값이다`() {
        assertThat(detail().availableQuantity).isEqualTo(20)
    }

    @Test
    fun `접근 범위가 전체이고 창고를 지정하지 않으면 창고 조건 없이 조회한다`() = runTest {
        val query = LocationStockQuery(productId = 100L)
        whenever(currentWarehouseAccessProvider.current()).thenReturn(AllWarehouses)
        whenever(locationStockQueryRepository.findAll(query)).thenReturn(flowOf(detail()))

        val result = locationStockQueryService.getAll(query).toList()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `접근 범위가 전체이면 요청한 창고를 그대로 전달한다`() = runTest {
        val query = LocationStockQuery(warehouseIds = listOf(1L, 2L), qualityStatus = StockQualityStatus.DEFECTIVE)
        whenever(currentWarehouseAccessProvider.current()).thenReturn(AllWarehouses)
        whenever(locationStockQueryRepository.findAll(query)).thenReturn(flowOf(detail()))

        val result = locationStockQueryService.getAll(query).toList()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `접근 범위가 제한적이면 창고를 지정하지 않아도 배정된 창고로 좁혀 조회한다`() = runTest {
        whenever(currentWarehouseAccessProvider.current()).thenReturn(OnlyWarehouses(setOf(2L)))
        whenever(locationStockQueryRepository.findAll(LocationStockQuery(warehouseIds = listOf(2L), zoneId = 5L)))
            .thenReturn(flowOf(detail()))

        val result = locationStockQueryService.getAll(LocationStockQuery(zoneId = 5L)).toList()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `접근 범위가 제한적이면 요청한 창고와의 교집합만 조회한다`() = runTest {
        whenever(currentWarehouseAccessProvider.current()).thenReturn(OnlyWarehouses(setOf(2L, 3L)))
        whenever(locationStockQueryRepository.findAll(LocationStockQuery(warehouseIds = listOf(2L))))
            .thenReturn(flowOf(detail()))

        val result = locationStockQueryService.getAll(LocationStockQuery(warehouseIds = listOf(1L, 2L))).toList()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `요청한 창고가 접근 범위와 겹치지 않으면 저장소를 조회하지 않고 빈 목록을 반환한다`() = runTest {
        whenever(currentWarehouseAccessProvider.current()).thenReturn(OnlyWarehouses(setOf(2L)))

        val result = locationStockQueryService.getAll(LocationStockQuery(warehouseIds = listOf(1L))).toList()

        assertThat(result).isEmpty()
        verify(locationStockQueryRepository, never()).findAll(any())
    }
}
