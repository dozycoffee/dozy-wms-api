package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.InvalidOutboundStatusTransitionException
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotCancelableException
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import com.dozycoffee.wms.support.SwitchableWarehouseAccess
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class OutboundCancelServiceTest {

    @Mock
    private lateinit var outboundRepository: OutboundRepository

    @Mock
    private lateinit var outboundItemRepository: OutboundItemRepository

    @Mock
    private lateinit var outboundAllocationRepository: OutboundAllocationRepository

    @Mock
    private lateinit var outboundAllocationHolder: OutboundAllocationHolder

    @Mock
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @Mock
    private lateinit var outboundReleaseCallRecorder: OutboundReleaseCallRecorder

    private val warehouseAccess = SwitchableWarehouseAccess()

    private lateinit var service: OutboundCancelService

    @BeforeEach
    fun setUp() {
        service = OutboundCancelService(
            WarehouseAccessGuard(warehouseAccess),
            outboundRepository,
            outboundItemRepository,
            outboundAllocationRepository,
            outboundAllocationHolder,
            getWorkAreaUseCase,
            releaseWorkAreaUseCase,
            outboundReleaseCallRecorder
        )
    }

    private fun workArea() = WorkAreaResult(
        1L, 1L, AreaCode.OUTBOUND, AreaCode.OUTBOUND.areaName, AreaCode.OUTBOUND.capacity.value, 10, AvailabilityStatus.AVAILABLE
    )

    private fun stub(status: OutboundStatus): Outbound {
        val existing = outbound().outboundId(1L).warehouseId(1L).reservationId(7L).status(status).build()
        runBlocking { whenever(outboundRepository.findById(1L)).thenReturn(existing) }
        return existing
    }

    @Test
    fun `피킹 전 출고는 취소 요청 즉시 취소하고 예약 해제를 기록한다`() = runTest {
        val existing = stub(OutboundStatus.REQUESTED)
        whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.requestCancel(1L)

        assertThat(result.status).isEqualTo(OutboundStatus.CANCELED)
        verify(outboundReleaseCallRecorder).record(existing)
        verify(outboundAllocationHolder, never()).release(any())
    }

    @Test
    fun `피킹 이후 취소 요청은 승인 대기가 되고 해제도 호출 기록도 아직 하지 않는다`() = runTest {
        stub(OutboundStatus.PICKING)
        whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.requestCancel(1L)

        assertThat(result.status).isEqualTo(OutboundStatus.CANCEL_REQUESTED)
        verify(outboundReleaseCallRecorder, never()).record(any())
        verify(outboundAllocationHolder, never()).release(any())
    }

    @Test
    fun `이미 취소를 요청했거나 취소된 출고는 현재 상태를 그대로 돌려준다`() = runTest {
        stub(OutboundStatus.CANCEL_REQUESTED)

        assertThat(service.requestCancel(1L).status).isEqualTo(OutboundStatus.CANCEL_REQUESTED)

        stub(OutboundStatus.CANCELED)
        assertThat(service.requestCancel(1L).status).isEqualTo(OutboundStatus.CANCELED)
        verify(outboundRepository, never()).save(any())
        verify(outboundReleaseCallRecorder, never()).record(any())
    }

    @Test
    fun `출고가 완료되면 취소할 수 없다`() = runTest {
        stub(OutboundStatus.COMPLETED)

        assertThatThrownBy { runBlocking { service.requestCancel(1L) } }.isInstanceOf(OutboundNotCancelableException::class.java)
    }

    @Test
    fun `취소 승인은 점유와 출고장 점유를 풀고 예약 해제를 기록한다`() = runTest {
        val existing = stub(OutboundStatus.CANCEL_REQUESTED)
        val item = outboundItem().outboundItemId(1L).outboundId(1L).requestedQuantity(10).pickedQuantity(8).build()
        whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item))
        val allocations = listOf(
            OutboundAllocation.reconstitute(1L, 1L, 11L, "A", null, 5, 5),
            OutboundAllocation.reconstitute(2L, 1L, 12L, "B", null, 3, 3)
        )
        whenever(outboundAllocationRepository.findAllByOutboundItemIds(listOf(1L))).thenReturn(allocations)
        whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.OUTBOUND)).thenReturn(workArea())
        whenever(releaseWorkAreaUseCase.release(any())).thenReturn(workArea())
        whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.approveCancel(1L)

        assertThat(result.status).isEqualTo(OutboundStatus.CANCELED)
        allocations.forEach { verify(outboundAllocationHolder).release(it) }
        verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 8))
        verify(outboundReleaseCallRecorder).record(existing)
    }

    @Test
    fun `피킹 수량이 없으면 출고장 점유는 건드리지 않는다`() = runTest {
        stub(OutboundStatus.CANCEL_REQUESTED)
        val item = outboundItem().outboundItemId(1L).outboundId(1L).requestedQuantity(10).pickedQuantity(0).build()
        whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item))
        whenever(outboundAllocationRepository.findAllByOutboundItemIds(listOf(1L))).thenReturn(emptyList())
        whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

        service.approveCancel(1L)

        verify(releaseWorkAreaUseCase, never()).release(any())
    }

    @Test
    fun `이미 취소된 출고를 다시 승인해도 아무것도 하지 않는다`() = runTest {
        stub(OutboundStatus.CANCELED)

        assertThat(service.approveCancel(1L).status).isEqualTo(OutboundStatus.CANCELED)
        verify(outboundReleaseCallRecorder, never()).record(any())
    }

    @Test
    fun `취소 요청이 없는 출고는 승인할 수 없다`() = runTest {
        stub(OutboundStatus.PICKING)

        assertThatThrownBy { runBlocking { service.approveCancel(1L) } }
            .isInstanceOf(InvalidOutboundStatusTransitionException::class.java)
    }

    @Test
    fun `존재하지 않는 출고이거나 접근할 수 없는 창고이면 예외를 던진다`() = runTest {
        whenever(outboundRepository.findById(1L)).thenReturn(null)
            .thenReturn(outbound().outboundId(1L).warehouseId(1L).status(OutboundStatus.REQUESTED).build())

        assertThatThrownBy { runBlocking { service.requestCancel(1L) } }.isInstanceOf(OutboundNotFoundException::class.java)

        warehouseAccess.access = OnlyWarehouses(setOf(2L))
        assertThatThrownBy { runBlocking { service.requestCancel(1L) } }.isInstanceOf(WarehouseAccessDeniedException::class.java)
    }
}
