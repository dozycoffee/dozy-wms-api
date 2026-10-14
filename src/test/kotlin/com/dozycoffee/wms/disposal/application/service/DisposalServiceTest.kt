package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.application.port.out.DisposalRepository
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.exception.DisposalNotFoundException
import com.dozycoffee.wms.disposal.domain.exception.DisposalQuantityExceededException
import com.dozycoffee.wms.disposal.domain.exception.DisposalStockWarehouseMismatchException
import com.dozycoffee.wms.disposal.domain.exception.InventoryNotDisposableException
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalItemTestBuilder.Companion.disposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalTestBuilder.Companion.disposal
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.location_stock.application.port.`in`.DecreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.GetLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.support.SwitchableWarehouseAccess
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class DisposalServiceTest {

    @Mock
    private lateinit var disposalRepository: DisposalRepository

    @Mock
    private lateinit var disposalItemRepository: DisposalItemRepository

    @Mock
    private lateinit var getLocationStockUseCase: GetLocationStockUseCase

    @Mock
    private lateinit var decreaseLocationStockUseCase: DecreaseLocationStockUseCase

    @Mock
    private lateinit var getLocationUseCase: GetLocationUseCase

    @Mock
    private lateinit var getZoneUseCase: GetZoneUseCase

    @Mock
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @Mock
    private lateinit var disposalInventoryCallRecorder: DisposalInventoryCallRecorder

    private val warehouseAccess = SwitchableWarehouseAccess()

    @Spy
    private var warehouseAccessGuard: WarehouseAccessGuard = WarehouseAccessGuard(warehouseAccess)

    @InjectMocks
    private lateinit var disposalService: DisposalService

    private fun workAreaResult(usedCapacity: Int): WorkAreaResult {
        return WorkAreaResult(
            1L, 1L, AreaCode.DISPOSAL, AreaCode.DISPOSAL.areaName, AreaCode.DISPOSAL.capacity.value, usedCapacity, AvailabilityStatus.AVAILABLE
        )
    }

    private fun stockResult(
        locationStockId: Long,
        quantity: Int,
        locationId: Long = 100L,
        qualityStatus: StockQualityStatus = StockQualityStatus.DISPOSAL_SCHEDULED
    ): LocationStockResult {
        return LocationStockResult(locationStockId, locationId, 100L, "LOT-1", qualityStatus, quantity)
    }

    private suspend fun stubWarehouseOf(locationId: Long, warehouseId: Long) {
        whenever(getLocationUseCase.getById(locationId))
            .thenReturn(LocationResult(locationId, 10L, "A-01", 70, 30, AvailabilityStatus.AVAILABLE))
        whenever(getZoneUseCase.getById(10L)).thenReturn(
            ZoneResult(10L, warehouseId, ZoneCode.A, "A Zone", TemperatureType.AMBIENT, 820, AvailabilityStatus.AVAILABLE)
        )
    }

    @Nested
    inner class 폐기_등록 {

        @Test
        fun `대상이 폐기예정 상태이고 수량이 위치 재고 이내면 REQUESTED 상태로 등록된다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 5, DisposalReason.EXPIRED)))
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getLocationStockUseCase.getById(10L)).thenReturn(stockResult(10L, 5))
            stubWarehouseOf(100L, 1L)
            whenever(disposalItemRepository.sumPendingQuantityByLocationStockId(10L)).thenReturn(0)

            val result = disposalService.register(command)

            assertThat(result.disposalId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(DisposalStatus.REQUESTED)
            verify(disposalItemRepository).save(any())
        }

        @Test
        fun `일부 수량만 폐기해도 등록된다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 3, DisposalReason.INSPECTION_DEFECT)))
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getLocationStockUseCase.getById(10L)).thenReturn(stockResult(10L, 5, qualityStatus = StockQualityStatus.DEFECTIVE))
            stubWarehouseOf(100L, 1L)
            whenever(disposalItemRepository.sumPendingQuantityByLocationStockId(10L)).thenReturn(0)

            disposalService.register(command)

            verify(disposalItemRepository).save(any())
        }

        @Test
        fun `정상 품질 재고는 폐기 대상으로 등록할 수 없다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 5, DisposalReason.EXPIRED)))
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getLocationStockUseCase.getById(10L)).thenReturn(stockResult(10L, 5, qualityStatus = StockQualityStatus.NORMAL))

            assertThatThrownBy { runBlocking { disposalService.register(command) } }
                .isInstanceOf(InventoryNotDisposableException::class.java)
        }

        @Test
        fun `폐기 수량이 위치 재고 수량을 넘으면 예외를 던진다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 6, DisposalReason.EXPIRED)))
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getLocationStockUseCase.getById(10L)).thenReturn(stockResult(10L, 5))
            stubWarehouseOf(100L, 1L)
            whenever(disposalItemRepository.sumPendingQuantityByLocationStockId(10L)).thenReturn(0)

            assertThatThrownBy { runBlocking { disposalService.register(command) } }
                .isInstanceOf(DisposalQuantityExceededException::class.java)
        }

        @Test
        fun `다른 미완료 폐기 신청 수량을 더해 위치 재고를 넘으면 예외를 던진다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 3, DisposalReason.EXPIRED)))
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getLocationStockUseCase.getById(10L)).thenReturn(stockResult(10L, 5))
            stubWarehouseOf(100L, 1L)
            whenever(disposalItemRepository.sumPendingQuantityByLocationStockId(10L)).thenReturn(3)

            assertThatThrownBy { runBlocking { disposalService.register(command) } }
                .isInstanceOf(DisposalQuantityExceededException::class.java)
        }

        @Test
        fun `같은 요청 안에서 같은 위치 재고를 여러 번 담아도 합이 위치 재고를 넘으면 예외를 던진다`() = runTest {
            val command = RegisterDisposalCommand(
                1L,
                listOf(
                    RegisterDisposalItemCommand(10L, 3, DisposalReason.EXPIRED),
                    RegisterDisposalItemCommand(10L, 3, DisposalReason.OTHER)
                )
            )
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getLocationStockUseCase.getById(10L)).thenReturn(stockResult(10L, 5))
            stubWarehouseOf(100L, 1L)
            whenever(disposalItemRepository.sumPendingQuantityByLocationStockId(10L)).thenReturn(0)

            assertThatThrownBy { runBlocking { disposalService.register(command) } }
                .isInstanceOf(DisposalQuantityExceededException::class.java)
        }

        @Test
        fun `대상 재고가 요청한 창고에 속하지 않으면 예외를 던진다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 5, DisposalReason.EXPIRED)))
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getLocationStockUseCase.getById(10L)).thenReturn(stockResult(10L, 5))
            stubWarehouseOf(100L, 2L)

            assertThatThrownBy { runBlocking { disposalService.register(command) } }
                .isInstanceOf(DisposalStockWarehouseMismatchException::class.java)
        }
    }

    @Nested
    inner class 폐기_승인 {

        @Test
        fun `대상 수량만큼 폐기 처리장을 점유하고 APPROVED로 전환한다`() = runTest {
            val existingDisposal: Disposal = disposal().disposalId(1L).warehouseId(1L).status(DisposalStatus.REQUESTED).build()
            val item: DisposalItem = disposalItem().disposalItemId(1L).disposalId(1L).locationStockId(10L).quantity(5).build()
            whenever(disposalRepository.findById(1L)).thenReturn(existingDisposal)
            whenever(disposalItemRepository.findAllByDisposalId(1L)).thenReturn(flowOf(item))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.DISPOSAL)).thenReturn(workAreaResult(0))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(workAreaResult(5))
            whenever(disposalRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = disposalService.approve(1L)

            assertThat(result.status).isEqualTo(DisposalStatus.APPROVED)
            verify(occupyWorkAreaUseCase).occupy(OccupyWorkAreaCommand(1L, 5))
        }

        @Test
        fun `존재하지 않는 폐기를 승인하면 예외를 던진다`() = runTest {
            whenever(disposalRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { disposalService.approve(1L) } }
                .isInstanceOf(DisposalNotFoundException::class.java)
        }
    }

    @Nested
    inner class 폐기_완료 {

        @Test
        fun `폐기 수량만큼 위치 재고를 줄이고 폐기 처리장 점유를 해제하며 inventory 호출을 기록한다`() = runTest {
            val existingDisposal: Disposal = disposal().disposalId(1L).warehouseId(1L).status(DisposalStatus.APPROVED).build()
            val item: DisposalItem = disposalItem().disposalItemId(1L).disposalId(1L).locationStockId(10L).quantity(3).build()
            val stock = stockResult(10L, 5, 200L)
            whenever(disposalRepository.findById(1L)).thenReturn(existingDisposal)
            whenever(disposalItemRepository.findAllByDisposalId(1L)).thenReturn(flowOf(item))
            whenever(getLocationStockUseCase.getById(10L)).thenReturn(stock)
            whenever(decreaseLocationStockUseCase.decrease(any())).thenReturn(stockResult(10L, 2, 200L))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.DISPOSAL)).thenReturn(workAreaResult(3))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(workAreaResult(0))
            whenever(disposalRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = disposalService.complete(1L)

            assertThat(result.status).isEqualTo(DisposalStatus.COMPLETED)
            verify(decreaseLocationStockUseCase)
                .decrease(DecreaseLocationStockCommand(200L, 100L, "LOT-1", StockQualityStatus.DISPOSAL_SCHEDULED, 3))
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 3))
            verify(disposalInventoryCallRecorder).record(1L, item, stock)
        }

        @Test
        fun `존재하지 않는 폐기를 완료하면 예외를 던진다`() = runTest {
            whenever(disposalRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { disposalService.complete(1L) } }
                .isInstanceOf(DisposalNotFoundException::class.java)
        }
    }

    @Nested
    inner class 단건_조회 {

        @Test
        fun `존재하지 않는 폐기를 조회하면 예외를 던진다`() = runTest {
            whenever(disposalRepository.findById(999L)).thenReturn(null)

            assertThatThrownBy { runBlocking { disposalService.getById(999L) } }
                .isInstanceOf(DisposalNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고_접근 {

        @Test
        fun `접근할 수 없는 창고에는 등록할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))

            assertThatThrownBy { runBlocking { disposalService.register(RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 5, DisposalReason.EXPIRED)))) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(disposalRepository, never()).save(any())
        }

        @Test
        fun `접근할 수 없는 창고의 문서는 단건 조회할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))
            whenever(disposalRepository.findById(1L)).thenReturn(disposal().disposalId(1L).warehouseId(1L).build())

            assertThatThrownBy { runBlocking { disposalService.getById(1L) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
        }

        @Test
        fun `목록은 접근 가능한 창고로 좁혀 조회한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(1L, 3L))
            whenever(disposalRepository.findAll(null, listOf(1L, 3L))).thenReturn(flowOf())

            disposalService.getAll(null).toList()

            verify(disposalRepository).findAll(null, listOf(1L, 3L))
        }

        @Test
        fun `접근 가능한 창고가 없으면 저장소를 조회하지 않고 빈 목록을 반환한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(emptySet())

            val result = disposalService.getAll(null).toList()

            assertThat(result).isEmpty()
            verify(disposalRepository, never()).findAll(any(), any())
        }

        @Test
        fun `전체 창고 접근이면 창고 조건 없이 조회한다`() = runTest {
            whenever(disposalRepository.findAll(null, null)).thenReturn(flowOf())

            disposalService.getAll(null).toList()

            verify(disposalRepository).findAll(null, null)
        }
    }
}
