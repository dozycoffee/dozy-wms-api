package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.security.CurrentWarehouseAccessProvider
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.location_stock.application.port.`in`.result.WarehouseStockQuantityResult
import com.dozycoffee.wms.location_stock.application.port.out.WarehouseStockQuantityRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class WarehouseStockQuantityServiceTest {

    @Mock
    private lateinit var warehouseStockQuantityRepository: WarehouseStockQuantityRepository

    @Mock
    private lateinit var getWarehouseUseCase: GetWarehouseUseCase

    @Mock
    private lateinit var currentWarehouseAccessProvider: CurrentWarehouseAccessProvider

    private lateinit var service: WarehouseStockQuantityService

    private val quantities = listOf(
        WarehouseStockQuantityResult(1L, "L-1", StockQualityStatus.NORMAL, 30),
        WarehouseStockQuantityResult(1L, "L-1", StockQualityStatus.DEFECTIVE, 2)
    )

    @BeforeEach
    fun setUp() {
        service = WarehouseStockQuantityService(
            warehouseStockQuantityRepository,
            getWarehouseUseCase,
            WarehouseAccessGuard(currentWarehouseAccessProvider)
        )
    }

    @Test
    fun `접근 가능한 창고의 수량을 조건과 함께 조회한다`() = runTest {
        whenever(currentWarehouseAccessProvider.current()).thenReturn(OnlyWarehouses(setOf(10L)))
        whenever(warehouseStockQuantityRepository.sumByWarehouse(10L, 1L, "L-1")).thenReturn(quantities)

        val result = service.getQuantities(10L, 1L, "L-1")

        assertThat(result).isEqualTo(quantities)
        verify(getWarehouseUseCase).getById(10L)
    }

    @Test
    fun `접근할 수 없는 창고면 조회하지 않고 거부한다`() = runTest {
        whenever(currentWarehouseAccessProvider.current()).thenReturn(OnlyWarehouses(setOf(1L)))

        assertThatThrownBy { runBlocking { service.getQuantities(10L, null, null) } }
            .isInstanceOf(WarehouseAccessDeniedException::class.java)
        verify(warehouseStockQuantityRepository, never()).sumByWarehouse(any(), any(), any())
    }

    @Test
    fun `없는 창고는 빈 목록이 아니라 예외다`() = runTest {
        whenever(currentWarehouseAccessProvider.current()).thenReturn(OnlyWarehouses(setOf(10L)))
        whenever(getWarehouseUseCase.getById(10L)).thenThrow(WarehouseNotFoundException())

        assertThatThrownBy { runBlocking { service.getQuantities(10L, null, null) } }
            .isInstanceOf(WarehouseNotFoundException::class.java)
        verify(warehouseStockQuantityRepository, never()).sumByWarehouse(any(), any(), any())
    }
}
