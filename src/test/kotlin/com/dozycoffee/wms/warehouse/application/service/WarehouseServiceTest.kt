package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.support.SwitchableWarehouseAccess
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWarehouseCommand
import com.dozycoffee.wms.warehouse.application.port.out.WarehouseRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.Mockito.verify

@ExtendWith(MockitoExtension::class)
class WarehouseServiceTest {

    @Mock
    private lateinit var warehouseRepository: WarehouseRepository

    private val warehouseAccess = SwitchableWarehouseAccess()

    private lateinit var warehouseService: WarehouseService

    @BeforeEach
    fun setUp() {
        warehouseService = WarehouseService(warehouseRepository, WarehouseAccessGuard(warehouseAccess))
    }

    @Nested
    inner class 창고_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 창고 정보를 반환한다`() = runTest {
            val command = RegisterWarehouseCommand(
                "도지하우스 제주 센터", "제주특별자치도 제주시", BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312)
            )
            val saved: Warehouse = warehouse().warehouseId(1L).build()
            whenever(warehouseRepository.save(any())).thenReturn(saved)

            val result = warehouseService.register(command)

            assertThat(result.warehouseId).isEqualTo(1L)
            assertThat(result.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)

            val captor = argumentCaptor<Warehouse>()

            verify(warehouseRepository).save(captor.capture())
            assertThat(captor.firstValue.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }
    }

    @Nested
    inner class 창고_활성화 {

        @Test
        fun `존재하는 창고를 활성화하면 상태가 AVAILABLE로 바뀐다`() = runTest {
            val inactive: Warehouse = warehouse().warehouseId(1L).warehouseStatus(AvailabilityStatus.UNAVAILABLE).build()
            whenever(warehouseRepository.findById(1L)).thenReturn(inactive)
            whenever(warehouseRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            val result = warehouseService.activate(1L)

            assertThat(result.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }

        @Test
        fun `존재하지 않는 창고를 활성화하면 예외를 던진다`() = runTest {
            whenever(warehouseRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { warehouseService.activate(1L) } }
                .isInstanceOf(WarehouseNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고_비활성화 {

        @Test
        fun `존재하는 창고를 비활성화하면 상태가 UNAVAILABLE로 바뀐다`() = runTest {
            val active: Warehouse = warehouse().warehouseId(1L).warehouseStatus(AvailabilityStatus.AVAILABLE).build()
            whenever(warehouseRepository.findById(1L)).thenReturn(active)
            whenever(warehouseRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            val result = warehouseService.deactivate(1L)

            assertThat(result.warehouseStatus).isEqualTo(AvailabilityStatus.UNAVAILABLE)
        }

        @Test
        fun `존재하지 않는 창고를 비활성화하면 예외를 던진다`() = runTest {
            whenever(warehouseRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { warehouseService.deactivate(1L) } }
                .isInstanceOf(WarehouseNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고_단건_조회 {

        @Test
        fun `존재하는 창고를 조회하면 결과를 반환한다`() = runTest {
            val found: Warehouse = warehouse().warehouseId(1L).build()
            whenever(warehouseRepository.findById(1L)).thenReturn(found)

            val result = warehouseService.getById(1L)

            assertThat(result.warehouseId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 창고를 조회하면 예외를 던진다`() = runTest {
            whenever(warehouseRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { warehouseService.getById(1L) } }
                .isInstanceOf(WarehouseNotFoundException::class.java)
        }

        @Test
        fun `배정된 창고이면 조회할 수 있다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(1L))
            whenever(warehouseRepository.findById(1L)).thenReturn(warehouse().warehouseId(1L).build())

            assertThat(warehouseService.getById(1L).warehouseId).isEqualTo(1L)
        }

        @Test
        fun `배정되지 않은 창고를 조회하면 접근 거부 예외를 던지고 저장소를 조회하지 않는다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))

            assertThatThrownBy { runBlocking { warehouseService.getById(1L) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(warehouseRepository, never()).findById(any())
        }
    }

    @Nested
    inner class 창고_목록_조회 {

        private fun stubFindAll(warehouseIds: List<Long>?, vararg found: Warehouse) {
            whenever(warehouseRepository.findAll(warehouseIds)).thenReturn(flowOf(*found))
        }

        @Test
        fun `전체 접근이면 조건 없이 모든 창고를 조회한다`() = runTest {
            stubFindAll(null, warehouse().warehouseId(1L).build(), warehouse().warehouseId(2L).build())

            val result = warehouseService.getAll().toList()

            assertThat(result.map { it.warehouseId }).containsExactly(1L, 2L)
        }

        @Test
        fun `일부 창고만 배정되면 배정된 창고 ID로 조회한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))
            stubFindAll(listOf(2L), warehouse().warehouseId(2L).build())

            val result = warehouseService.getAll().toList()

            assertThat(result.map { it.warehouseId }).containsExactly(2L)
        }

        @Test
        fun `배정된 창고가 없으면 저장소를 조회하지 않고 빈 목록을 반환한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(emptySet())

            val result = warehouseService.getAll().toList()

            assertThat(result).isEmpty()
            verify(warehouseRepository, never()).findAll(any())
        }
    }
}
