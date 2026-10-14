package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import com.dozycoffee.wms.support.SwitchableWarehouseAccess
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class OutboundServiceTest {

    @Mock
    private lateinit var outboundRepository: OutboundRepository

    @Mock
    private lateinit var outboundItemRepository: OutboundItemRepository

    @Mock
    private lateinit var outboundAllocationRepository: OutboundAllocationRepository

    @Mock
    private lateinit var outboundFulfillmentCallRecorder: OutboundFulfillmentCallRecorder

    @Mock
    private lateinit var outboundAllocationHolder: OutboundAllocationHolder

    @Mock
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @Mock
    private lateinit var releaseLocationUseCase: ReleaseLocationUseCase

    private val warehouseAccess = SwitchableWarehouseAccess()

    @Spy
    private var warehouseAccessGuard: WarehouseAccessGuard = WarehouseAccessGuard(warehouseAccess)

    @InjectMocks
    private lateinit var outboundService: OutboundService

    private fun workAreaResult(usedCapacity: Int): WorkAreaResult {
        return WorkAreaResult(
            1L, 1L, AreaCode.OUTBOUND, AreaCode.OUTBOUND.areaName, AreaCode.OUTBOUND.capacity.value, usedCapacity, AvailabilityStatus.AVAILABLE
        )
    }

    private fun locationResult(locationId: Long): LocationResult {
        return LocationResult(locationId, 10L, "A-0$locationId", 70, 30, AvailabilityStatus.AVAILABLE)
    }

    @Nested
    inner class 피킹_시작 {

        private fun allocation(id: Long, inventoryId: Long, lotNumber: String, quantity: Int): OutboundAllocation =
            OutboundAllocation.reconstitute(id, 1L, inventoryId, lotNumber, null, quantity, null)

        private suspend fun stubPicking(allocations: List<OutboundAllocation>, requestedQuantity: Int = 15) {
            val existingOutbound: Outbound = outbound().outboundId(1L).warehouseId(1L).status(OutboundStatus.REQUESTED).build()
            val item: OutboundItem =
                outboundItem().outboundItemId(1L).outboundId(1L).productId(100L).requestedQuantity(requestedQuantity).build()
            whenever(outboundRepository.findById(1L)).thenReturn(existingOutbound)
            whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item))
            whenever(outboundAllocationRepository.findAllByOutboundItemIds(listOf(1L))).thenReturn(allocations)
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.OUTBOUND)).thenReturn(workAreaResult(0))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(workAreaResult(15))
            whenever(outboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(outboundAllocationRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }
        }

        @Test
        fun `확정 예약이 할당한 Lot별 수량만큼 점유하고 출고장을 점유한다`() = runTest {
            val allocations = listOf(allocation(1L, 11L, "LOT-10", 5), allocation(2L, 12L, "LOT-20", 10))
            stubPicking(allocations)
            whenever(outboundAllocationHolder.hold(any(), any(), eq(1L))).thenAnswer { (it.getArgument(1) as OutboundAllocation).quantity }

            val result = outboundService.startPicking(1L)

            assertThat(result.status).isEqualTo(OutboundStatus.PICKING)
            verify(outboundAllocationHolder, times(2)).hold(any(), any(), eq(1L))
            verify(occupyWorkAreaUseCase).occupy(OccupyWorkAreaCommand(1L, 15))
            val allocationCaptor = argumentCaptor<OutboundAllocation>()
            verify(outboundAllocationRepository, times(2)).save(allocationCaptor.capture())
            assertThat(allocationCaptor.allValues.map { it.pickedQuantity }).containsExactly(5, 10)
            val itemCaptor = argumentCaptor<OutboundItem>()
            verify(outboundItemRepository).save(itemCaptor.capture())
            assertThat(itemCaptor.firstValue.pickedQuantity).isEqualTo(15)
        }

        @Test
        fun `가용 재고가 부족해 일부만 점유되면 확보한 만큼만 피킹한다`() = runTest {
            stubPicking(listOf(allocation(1L, 11L, "LOT-10", 15)))
            whenever(outboundAllocationHolder.hold(any(), any(), eq(1L))).thenReturn(10)

            outboundService.startPicking(1L)

            val itemCaptor = argumentCaptor<OutboundItem>()
            verify(outboundItemRepository).save(itemCaptor.capture())
            assertThat(itemCaptor.firstValue.pickedQuantity).isEqualTo(10)
            assertThat(itemCaptor.firstValue.shortageQuantity).isEqualTo(5)
            verify(occupyWorkAreaUseCase).occupy(OccupyWorkAreaCommand(1L, 10))
        }

        @Test
        fun `존재하지 않는 출고를 피킹 시작하면 예외를 던진다`() = runTest {
            whenever(outboundRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { outboundService.startPicking(1L) } }
                .isInstanceOf(OutboundNotFoundException::class.java)
        }
    }

    @Nested
    inner class 검수_시작 {

        @Test
        fun `INSPECTING으로 전환한다`() = runTest {
            val existingOutbound: Outbound = outbound().outboundId(1L).status(OutboundStatus.PICKING).build()
            whenever(outboundRepository.findById(1L)).thenReturn(existingOutbound)
            whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = outboundService.startInspecting(1L)

            assertThat(result.status).isEqualTo(OutboundStatus.INSPECTING)
        }
    }

    @Nested
    inner class 출고_완료 {

        @Test
        fun `피킹 계획대로 위치 재고를 꺼내고 출고장 점유를 해제하며 inventory 호출을 기록한다`() = runTest {
            val existingOutbound: Outbound = outbound().outboundId(1L).warehouseId(1L).status(OutboundStatus.INSPECTING).build()
            val item: OutboundItem = outboundItem()
                .outboundItemId(1L).outboundId(1L).productId(100L).requestedQuantity(15).pickedQuantity(15).build()
            val allocations = listOf(
                OutboundAllocation.reconstitute(1L, 1L, 11L, "LOT-10", null, 5, 5),
                OutboundAllocation.reconstitute(2L, 1L, 12L, "LOT-20", null, 10, 10)
            )
            whenever(outboundRepository.findById(1L)).thenReturn(existingOutbound)
            whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item))
            whenever(outboundAllocationRepository.findAllByOutboundItemIds(listOf(1L))).thenReturn(allocations)
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.OUTBOUND)).thenReturn(workAreaResult(15))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(workAreaResult(0))
            whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = outboundService.complete(1L)

            assertThat(result.status).isEqualTo(OutboundStatus.COMPLETED)
            allocations.forEach { verify(outboundAllocationHolder).consume(it) }
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 15))
            verify(outboundFulfillmentCallRecorder).record(existingOutbound, allocations)
        }

        @Test
        fun `존재하지 않는 출고를 완료하면 예외를 던진다`() = runTest {
            whenever(outboundRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { outboundService.complete(1L) } }
                .isInstanceOf(OutboundNotFoundException::class.java)
        }
    }

    @Nested
    inner class 단건_조회 {

        @Test
        fun `존재하지 않는 출고를 조회하면 예외를 던진다`() = runTest {
            whenever(outboundRepository.findById(999L)).thenReturn(null)

            assertThatThrownBy { runBlocking { outboundService.getById(999L) } }
                .isInstanceOf(OutboundNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고_접근 {

        @Test
        fun `접근할 수 없는 창고의 문서는 단건 조회할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))
            whenever(outboundRepository.findById(1L)).thenReturn(outbound().outboundId(1L).warehouseId(1L).build())

            assertThatThrownBy { runBlocking { outboundService.getById(1L) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
        }

        @Test
        fun `목록은 접근 가능한 창고로 좁혀 조회한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(1L, 3L))
            whenever(outboundRepository.findAll(null, listOf(1L, 3L))).thenReturn(flowOf())

            outboundService.getAll(null).toList()

            verify(outboundRepository).findAll(null, listOf(1L, 3L))
        }

        @Test
        fun `접근 가능한 창고가 없으면 저장소를 조회하지 않고 빈 목록을 반환한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(emptySet())

            val result = outboundService.getAll(null).toList()

            assertThat(result).isEmpty()
            verify(outboundRepository, never()).findAll(any(), any())
        }

        @Test
        fun `전체 창고 접근이면 창고 조건 없이 조회한다`() = runTest {
            whenever(outboundRepository.findAll(null, null)).thenReturn(flowOf())

            outboundService.getAll(null).toList()

            verify(outboundRepository).findAll(null, null)
        }
    }
}
