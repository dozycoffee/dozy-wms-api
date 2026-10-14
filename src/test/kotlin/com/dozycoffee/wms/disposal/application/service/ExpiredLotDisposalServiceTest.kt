package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterExpiredLotDisposalCommand
import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.application.port.out.DisposalRepository
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalTestBuilder.Companion.disposal
import com.dozycoffee.wms.location_stock.application.port.`in`.ScheduleLocationStockDisposalUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.result.HeldLocationStockResult
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ExpiredLotDisposalServiceTest {

    @Mock
    private lateinit var disposalRepository: DisposalRepository

    @Mock
    private lateinit var disposalItemRepository: DisposalItemRepository

    @Mock
    private lateinit var scheduleLocationStockDisposalUseCase: ScheduleLocationStockDisposalUseCase

    @InjectMocks
    private lateinit var service: ExpiredLotDisposalService

    private fun command(vararg transfers: Pair<Long, Int>) = RegisterExpiredLotDisposalCommand(
        "event-1", 100L, "LOT-1", transfers.map { RegisterExpiredLotDisposalCommand.Transfer(it.first, it.second) }
    )

    @Test
    fun `창고별로 정상 재고를 폐기 예정으로 옮기고 옮긴 행마다 EXPIRED 폐기 상품을 만든다`() = runTest {
        whenever(disposalRepository.existsBySourceEventIdAndWarehouseId(any(), any())).thenReturn(false)
        whenever(scheduleLocationStockDisposalUseCase.moveToDisposalScheduled(1L, 100L, "LOT-1", 7))
            .thenReturn(listOf(HeldLocationStockResult(31L, 10L, 5), HeldLocationStockResult(32L, 20L, 2)))
        whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(900L).warehouseId(1L).build())
        whenever(disposalItemRepository.save(any())).thenAnswer { it.getArgument(0) }

        val created = service.register(command(1L to 7))

        assertThat(created).isEqualTo(1)
        val disposalCaptor = argumentCaptor<Disposal>()
        verify(disposalRepository).save(disposalCaptor.capture())
        assertThat(disposalCaptor.firstValue.warehouseId).isEqualTo(1L)
        assertThat(disposalCaptor.firstValue.sourceEventId).isEqualTo("event-1")
        val itemCaptor = argumentCaptor<DisposalItem>()
        verify(disposalItemRepository, times(2)).save(itemCaptor.capture())
        assertThat(itemCaptor.allValues.map { it.locationStockId to it.quantity }).containsExactly(31L to 5, 32L to 2)
        assertThat(itemCaptor.allValues.map { it.reason }).containsOnly(DisposalReason.EXPIRED)
        assertThat(itemCaptor.allValues.map { it.disposalId }).containsOnly(900L)
    }

    @Test
    fun `이미 처리한 이벤트와 창고는 건너뛴다`() = runTest {
        whenever(disposalRepository.existsBySourceEventIdAndWarehouseId("event-1", 1L)).thenReturn(true)

        assertThat(service.register(command(1L to 7))).isEqualTo(0)
        verify(scheduleLocationStockDisposalUseCase, never()).moveToDisposalScheduled(any(), any(), any(), any())
        verify(disposalRepository, never()).save(any())
    }

    @Test
    fun `옮길 정상 재고가 없으면 폐기 신청을 만들지 않는다`() = runTest {
        whenever(disposalRepository.existsBySourceEventIdAndWarehouseId(any(), any())).thenReturn(false)
        whenever(scheduleLocationStockDisposalUseCase.moveToDisposalScheduled(1L, 100L, "LOT-1", 7)).thenReturn(emptyList())

        assertThat(service.register(command(1L to 7))).isEqualTo(0)
        verify(disposalRepository, never()).save(any())
    }

    @Test
    fun `정상 재고가 이벤트 수량보다 적어도 옮긴 만큼으로 폐기 신청을 만든다`() = runTest {
        whenever(disposalRepository.existsBySourceEventIdAndWarehouseId(any(), any())).thenReturn(false)
        whenever(scheduleLocationStockDisposalUseCase.moveToDisposalScheduled(1L, 100L, "LOT-1", 7))
            .thenReturn(listOf(HeldLocationStockResult(31L, 10L, 3)))
        whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(900L).warehouseId(1L).build())
        whenever(disposalItemRepository.save(any())).thenAnswer { it.getArgument(0) }

        assertThat(service.register(command(1L to 7))).isEqualTo(1)
        val itemCaptor = argumentCaptor<DisposalItem>()
        verify(disposalItemRepository).save(itemCaptor.capture())
        assertThat(itemCaptor.firstValue.quantity).isEqualTo(3)
    }
}
