package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.outbound.application.port.`in`.command.ReportPickingShortageCommand
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.InvalidReallocationException
import com.dozycoffee.wms.outbound.domain.exception.OutboundInventoryRejectedException
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotReallocatableException
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import com.dozycoffee.wms.support.SwitchableWarehouseAccess
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class OutboundReallocationServiceTest {

    @Mock
    private lateinit var outboundRepository: OutboundRepository

    @Mock
    private lateinit var outboundItemRepository: OutboundItemRepository

    @Mock
    private lateinit var outboundAllocationRepository: OutboundAllocationRepository

    @Mock
    private lateinit var outboundInventoryReallocator: OutboundInventoryReallocator

    @Mock
    private lateinit var outboundAllocationHolder: OutboundAllocationHolder

    @Mock
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    private val warehouseAccess = SwitchableWarehouseAccess()

    private lateinit var service: OutboundReallocationService

    @BeforeEach
    fun setUp() {
        service = OutboundReallocationService(
            WarehouseAccessGuard(warehouseAccess),
            outboundRepository,
            outboundItemRepository,
            outboundAllocationRepository,
            outboundInventoryReallocator,
            outboundAllocationHolder,
            getWorkAreaUseCase,
            occupyWorkAreaUseCase,
            releaseWorkAreaUseCase
        )
    }

    private fun pickingOutbound(count: Int = 0): Outbound =
        Outbound.reconstitute(1L, 1L, OutboundStatus.PICKING, 7L, "OMS", "ORDER-1", count)

    private fun item(picked: Int? = 6): OutboundItem =
        outboundItem().outboundItemId(1L).outboundId(1L).productId(100L).requestedQuantity(6).pickedQuantity(picked).build()

    private fun allocation(id: Long, inventoryId: Long, lotNumber: String, quantity: Int, picked: Int? = quantity) =
        OutboundAllocation.reconstitute(id, 1L, inventoryId, lotNumber, null, quantity, picked)

    private var allocationSequence: Long = 100L

    /** 저장소가 새 행에 ID를 채워 돌려주는 동작을 흉내 낸다 */
    private fun persisted(allocation: OutboundAllocation): OutboundAllocation {
        if (allocation.outboundAllocationId != null) return allocation
        return OutboundAllocation.reconstitute(
            ++allocationSequence,
            allocation.outboundItemId,
            allocation.inventoryId,
            allocation.lotNumber,
            allocation.expirationDate,
            allocation.quantity,
            allocation.pickedQuantity
        )
    }

    private fun workArea() = WorkAreaResult(
        1L, 1L, AreaCode.OUTBOUND, AreaCode.OUTBOUND.areaName, AreaCode.OUTBOUND.capacity.value, 6, AvailabilityStatus.AVAILABLE
    )

    private suspend fun stubExisting(
        allocations: List<OutboundAllocation>,
        outbound: Outbound = pickingOutbound(),
        pickedQuantity: Int? = 6
    ) {
        whenever(outboundRepository.findById(1L)).thenReturn(outbound)
        whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item(pickedQuantity)))
        whenever(outboundAllocationRepository.findAllByOutboundItemIds(listOf(1L))).thenReturn(allocations)
    }

    private fun command(vararg shortages: Pair<Long, Int>) =
        ReportPickingShortageCommand(1L, shortages.map { ReportPickingShortageCommand.Shortage(it.first, it.second) })

    @Test
    fun `부족 Lot의 수량을 다른 Lot으로 옮기면 할당을 갱신하고 점유를 다시 잡는다`() = runTest {
        val a = allocation(1L, 55L, "A", 4)
        val b = allocation(2L, 56L, "B", 2)
        stubExisting(listOf(a, b))
        whenever(outboundInventoryReallocator.reallocate(eq(7L), eq("wms-reallocation-1-1"), any())).thenReturn(
            listOf(
                OutboundInventoryReallocator.ReallocatedItem(
                    100L,
                    listOf(
                        OutboundInventoryReallocator.ReallocatedAllocation(55L, "A", null, 2),
                        OutboundInventoryReallocator.ReallocatedAllocation(56L, "B", null, 2),
                        OutboundInventoryReallocator.ReallocatedAllocation(57L, "C", LocalDate.of(2027, 5, 1), 2)
                    )
                )
            )
        )
        whenever(outboundAllocationHolder.hold(any(), any(), eq(1L))).thenAnswer { (it.getArgument(1) as OutboundAllocation).quantity }
        whenever(outboundAllocationRepository.save(any())).thenAnswer { persisted(it.getArgument(0)) }
        whenever(outboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

        val results = service.report(command(1L to 2))

        assertThat(results.map { it.inventoryId }).containsExactly(55L, 56L, 57L)
        assertThat(results.map { it.quantity }).containsExactly(2, 2, 2)
        assertThat(results.map { it.pickedQuantity }).containsExactly(2, 2, 2)
        verify(outboundAllocationHolder).release(a)
        verify(outboundAllocationHolder).release(b)
        val reportedBody = argumentCaptor<List<OutboundInventoryReallocator.Shortage>>()
        verify(outboundInventoryReallocator).reallocate(eq(7L), eq("wms-reallocation-1-1"), reportedBody.capture())
        assertThat(reportedBody.firstValue).containsExactly(OutboundInventoryReallocator.Shortage(55L, 2))
        val outboundCaptor = argumentCaptor<Outbound>()
        verify(outboundRepository).save(outboundCaptor.capture())
        assertThat(outboundCaptor.firstValue.reallocationCount).isEqualTo(1)
        verify(occupyWorkAreaUseCase, never()).occupy(any())
        verify(releaseWorkAreaUseCase, never()).release(any())
    }

    @Test
    fun `새로 생긴 할당은 저장해 ID를 받은 뒤에 점유한다`() = runTest {
        val a = allocation(1L, 55L, "A", 6)
        stubExisting(listOf(a))
        whenever(outboundInventoryReallocator.reallocate(eq(7L), eq("wms-reallocation-1-1"), any())).thenReturn(
            listOf(
                OutboundInventoryReallocator.ReallocatedItem(
                    100L,
                    listOf(
                        OutboundInventoryReallocator.ReallocatedAllocation(55L, "A", null, 2),
                        OutboundInventoryReallocator.ReallocatedAllocation(57L, "C", LocalDate.of(2027, 5, 1), 4)
                    )
                )
            )
        )
        whenever(outboundAllocationHolder.hold(any(), any(), eq(1L))).thenAnswer {
            val held = it.getArgument<OutboundAllocation>(1)
            requireNotNull(held.outboundAllocationId) { "ID가 없는 할당은 점유할 수 없다" }
            held.quantity
        }
        whenever(outboundAllocationRepository.save(any())).thenAnswer { persisted(it.getArgument(0)) }
        whenever(outboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

        val results = service.report(command(1L to 4))

        assertThat(results.map { it.inventoryId }).containsExactly(55L, 57L)
        assertThat(results.map { it.pickedQuantity }).containsExactly(2, 4)
    }

    @Test
    fun `응답에서 빠진 할당은 지우고 줄어든 피킹 수량만큼 출고장 점유를 해제한다`() = runTest {
        val a = allocation(1L, 55L, "A", 4)
        val b = allocation(2L, 56L, "B", 2)
        stubExisting(listOf(a, b))
        whenever(outboundInventoryReallocator.reallocate(any(), any(), any())).thenReturn(
            listOf(
                OutboundInventoryReallocator.ReallocatedItem(
                    100L,
                    listOf(OutboundInventoryReallocator.ReallocatedAllocation(56L, "B", null, 2))
                )
            )
        )
        whenever(outboundAllocationHolder.hold(any(), any(), eq(1L))).thenReturn(1)
        whenever(outboundAllocationRepository.save(any())).thenAnswer { persisted(it.getArgument(0)) }
        whenever(outboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.OUTBOUND)).thenReturn(workArea())
        whenever(releaseWorkAreaUseCase.release(any())).thenReturn(workArea())

        val results = service.report(command(1L to 4))

        verify(outboundAllocationRepository).delete(a)
        assertThat(results.map { it.inventoryId }).containsExactly(56L)
        verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 5))
        val itemCaptor = argumentCaptor<OutboundItem>()
        verify(outboundItemRepository).save(itemCaptor.capture())
        assertThat(itemCaptor.firstValue.pickedQuantity).isEqualTo(1)
    }

    @Test
    fun `대체 Lot을 더 많이 점유하면 늘어난 피킹 수량만큼 출고장을 점유한다`() = runTest {
        val a = allocation(1L, 55L, "A", 4, 1)
        stubExisting(listOf(a), pickedQuantity = 1)
        whenever(outboundInventoryReallocator.reallocate(any(), any(), any())).thenReturn(
            listOf(
                OutboundInventoryReallocator.ReallocatedItem(
                    100L,
                    listOf(
                        OutboundInventoryReallocator.ReallocatedAllocation(55L, "A", null, 1),
                        OutboundInventoryReallocator.ReallocatedAllocation(60L, "Z", null, 3)
                    )
                )
            )
        )
        whenever(outboundAllocationHolder.hold(any(), any(), eq(1L))).thenAnswer { (it.getArgument(1) as OutboundAllocation).quantity }
        whenever(outboundAllocationRepository.save(any())).thenAnswer { persisted(it.getArgument(0)) }
        whenever(outboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.OUTBOUND)).thenReturn(workArea())
        whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(workArea())

        service.report(command(1L to 3))

        verify(occupyWorkAreaUseCase, times(1)).occupy(OccupyWorkAreaCommand(1L, 3))
    }

    @Test
    fun `inventory가 거부하면 로컬 할당과 회차를 바꾸지 않는다`() = runTest {
        stubExisting(listOf(allocation(1L, 55L, "A", 4)))
        whenever(outboundInventoryReallocator.reallocate(any(), any(), any())).thenThrow(OutboundInventoryRejectedException())

        assertThatThrownBy { runBlocking { service.report(command(1L to 2)) } }
            .isInstanceOf(OutboundInventoryRejectedException::class.java)
        verify(outboundAllocationHolder, never()).release(any())
        verify(outboundAllocationRepository, never()).save(any())
        verify(outboundAllocationRepository, never()).delete(any())
        verify(outboundRepository, never()).save(any())
    }

    @Test
    fun `이전 재할당 횟수가 멱등 키 회차에 반영된다`() = runTest {
        stubExisting(listOf(allocation(1L, 55L, "A", 4)), pickingOutbound(count = 2))
        whenever(outboundInventoryReallocator.reallocate(any(), eq("wms-reallocation-1-3"), any()))
            .thenThrow(OutboundInventoryRejectedException())

        assertThatThrownBy { runBlocking { service.report(command(1L to 2)) } }
            .isInstanceOf(OutboundInventoryRejectedException::class.java)
        verify(outboundInventoryReallocator).reallocate(eq(7L), eq("wms-reallocation-1-3"), any())
    }

    @Test
    fun `이 출고의 할당이 아니거나 부족 수량이 범위를 벗어나거나 중복이면 예외를 던진다`() = runTest {
        stubExisting(listOf(allocation(1L, 55L, "A", 4)))

        assertThatThrownBy { runBlocking { service.report(command(99L to 1)) } }.isInstanceOf(InvalidReallocationException::class.java)
        assertThatThrownBy { runBlocking { service.report(command(1L to 5)) } }.isInstanceOf(InvalidReallocationException::class.java)
        assertThatThrownBy { runBlocking { service.report(command(1L to 0)) } }.isInstanceOf(InvalidReallocationException::class.java)
        assertThatThrownBy { runBlocking { service.report(command(1L to 1, 1L to 2)) } }
            .isInstanceOf(InvalidReallocationException::class.java)
        assertThatThrownBy { runBlocking { service.report(ReportPickingShortageCommand(1L, emptyList())) } }
            .isInstanceOf(InvalidReallocationException::class.java)
        verify(outboundInventoryReallocator, never()).reallocate(any(), any(), any())
    }

    @Test
    fun `피킹 중이 아니거나 예약 정보가 없는 출고는 재할당할 수 없다`() = runTest {
        whenever(outboundRepository.findById(1L))
            .thenReturn(Outbound.reconstitute(1L, 1L, OutboundStatus.INSPECTING, 7L, "OMS", "O", 0))
            .thenReturn(Outbound.reconstitute(1L, 1L, OutboundStatus.PICKING, null, null, null, 0))

        assertThatThrownBy { runBlocking { service.report(command(1L to 1)) } }
            .isInstanceOf(OutboundNotReallocatableException::class.java)
        assertThatThrownBy { runBlocking { service.report(command(1L to 1)) } }
            .isInstanceOf(OutboundNotReallocatableException::class.java)
    }

    @Test
    fun `존재하지 않는 출고이거나 접근할 수 없는 창고이면 예외를 던진다`() = runTest {
        whenever(outboundRepository.findById(1L)).thenReturn(null).thenReturn(pickingOutbound())

        assertThatThrownBy { runBlocking { service.report(command(1L to 1)) } }.isInstanceOf(OutboundNotFoundException::class.java)

        warehouseAccess.access = OnlyWarehouses(setOf(2L))
        assertThatThrownBy { runBlocking { service.report(command(1L to 1)) } }.isInstanceOf(WarehouseAccessDeniedException::class.java)
    }
}
