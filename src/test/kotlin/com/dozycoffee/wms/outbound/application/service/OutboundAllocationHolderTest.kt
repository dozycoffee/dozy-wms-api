package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.location_stock.application.port.`in`.HoldLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.result.HeldLocationStockResult
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationPickRepository
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocationPick
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
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
class OutboundAllocationHolderTest {

    @Mock
    private lateinit var holdLocationStockUseCase: HoldLocationStockUseCase

    @Mock
    private lateinit var outboundAllocationPickRepository: OutboundAllocationPickRepository

    @InjectMocks
    private lateinit var holder: OutboundAllocationHolder

    private val item = outboundItem().outboundItemId(1L).outboundId(1L).productId(100L).requestedQuantity(15).build()

    private fun allocation(quantity: Int) = OutboundAllocation.reconstitute(7L, 1L, 11L, "LOT-10", null, quantity, null)

    @Test
    fun `할당된 Lot을 점유하고 점유한 행마다 피킹 계획을 남긴다`() = runTest {
        whenever(holdLocationStockUseCase.holdForOutbound(1L, 100L, "LOT-10", 5)).thenReturn(
            listOf(HeldLocationStockResult(31L, 1L, 3), HeldLocationStockResult(32L, 2L, 2))
        )
        whenever(outboundAllocationPickRepository.save(any())).thenAnswer { it.getArgument(0) }

        val picked = holder.hold(item, allocation(5), 1L)

        assertThat(picked).isEqualTo(5)
        val captor = argumentCaptor<OutboundAllocationPick>()
        verify(outboundAllocationPickRepository, times(2)).save(captor.capture())
        assertThat(captor.allValues.map { it.outboundAllocationId }).containsOnly(7L)
        assertThat(captor.allValues.map { it.locationStockId to it.quantity }).containsExactly(31L to 3, 32L to 2)
    }

    @Test
    fun `가용 재고가 모자라면 점유한 만큼만 돌려준다`() = runTest {
        whenever(holdLocationStockUseCase.holdForOutbound(1L, 100L, "LOT-10", 15))
            .thenReturn(listOf(HeldLocationStockResult(31L, 1L, 10)))
        whenever(outboundAllocationPickRepository.save(any())).thenAnswer { it.getArgument(0) }

        assertThat(holder.hold(item, allocation(15), 1L)).isEqualTo(10)
    }

    @Test
    fun `점유할 재고가 없으면 0을 돌려주고 계획을 남기지 않는다`() = runTest {
        whenever(holdLocationStockUseCase.holdForOutbound(1L, 100L, "LOT-10", 5)).thenReturn(emptyList())

        assertThat(holder.hold(item, allocation(5), 1L)).isEqualTo(0)
        verify(outboundAllocationPickRepository, never()).save(any())
    }

    @Test
    fun `해제하면 계획의 점유를 풀고 계획을 지운다`() = runTest {
        whenever(outboundAllocationPickRepository.findAllByOutboundAllocationIds(listOf(7L))).thenReturn(
            listOf(OutboundAllocationPick.reconstitute(1L, 7L, 31L, 3), OutboundAllocationPick.reconstitute(2L, 7L, 32L, 2))
        )

        holder.release(allocation(5))

        verify(holdLocationStockUseCase).releaseHold(31L, 3)
        verify(holdLocationStockUseCase).releaseHold(32L, 2)
        verify(outboundAllocationPickRepository).deleteAllByOutboundAllocationId(7L)
    }

    @Test
    fun `소비하면 계획대로 위치 재고를 꺼낸다`() = runTest {
        whenever(outboundAllocationPickRepository.findAllByOutboundAllocationIds(listOf(7L))).thenReturn(
            listOf(OutboundAllocationPick.reconstitute(1L, 7L, 31L, 3))
        )

        holder.consume(allocation(3))

        verify(holdLocationStockUseCase).consume(31L, 3)
        verify(outboundAllocationPickRepository, never()).deleteAllByOutboundAllocationId(any())
    }
}
