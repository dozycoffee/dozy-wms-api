package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.HoldInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.HoldInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.AllocationResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var getLotUseCase: GetLotUseCase

    @Mock
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @Mock
    private lateinit var holdInventoryUseCase: HoldInventoryUseCase

    @InjectMocks
    private lateinit var holder: OutboundAllocationHolder

    private val item = outboundItem().outboundItemId(1L).outboundId(1L).productId(100L).requestedQuantity(15).build()

    private fun allocation(lotNumber: String, quantity: Int) =
        OutboundAllocation.reconstitute(1L, 1L, 11L, lotNumber, null, quantity, null)

    private fun lot(lotId: Long) = LotResult(lotId, "LOT-$lotId", 100L, null, null, LotStatus.NORMAL)

    private fun inventory(inventoryId: Long, lotId: Long, available: Int) =
        InventoryResult(inventoryId, 100L, lotId, 100L + inventoryId, available, 0, available, QualityStatus.NORMAL)

    private fun held(quantity: Int) = AllocationResult(900L, 1L, AllocationReferenceType.OUTBOUND, 1L, quantity, AllocationStatus.HELD)

    @Test
    fun `할당된 Lot의 재고만 재고 행 순서대로 점유한다`() = runTest {
        whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(flowOf(lot(10L), lot(20L)))
        whenever(getInventoryUseCase.getAll(null, 100L, QualityStatus.NORMAL, null, listOf(1L))).thenReturn(
            flowOf(inventory(7L, 10L, 10), inventory(3L, 10L, 3), inventory(5L, 20L, 50))
        )
        whenever(holdInventoryUseCase.hold(any())).thenReturn(held(3))

        val picked = holder.hold(item, allocation("LOT-10", 5), 1L)

        assertThat(picked).isEqualTo(5)
        val captor = argumentCaptor<HoldInventoryCommand>()
        verify(holdInventoryUseCase, times(2)).hold(captor.capture())
        assertThat(captor.firstValue).isEqualTo(HoldInventoryCommand(3L, AllocationReferenceType.OUTBOUND, 1L, 3))
        assertThat(captor.secondValue).isEqualTo(HoldInventoryCommand(7L, AllocationReferenceType.OUTBOUND, 1L, 2))
    }

    @Test
    fun `가용 재고가 부족하면 확보한 만큼만 돌려준다`() = runTest {
        whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(flowOf(lot(10L)))
        whenever(getInventoryUseCase.getAll(null, 100L, QualityStatus.NORMAL, null, listOf(1L)))
            .thenReturn(flowOf(inventory(1L, 10L, 10)))
        whenever(holdInventoryUseCase.hold(any())).thenReturn(held(10))

        assertThat(holder.hold(item, allocation("LOT-10", 15), 1L)).isEqualTo(10)
    }

    @Test
    fun `WMS에 없는 Lot이면 점유하지 않고 0을 돌려준다`() = runTest {
        whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(flowOf(lot(10L)))

        assertThat(holder.hold(item, allocation("LOT-UNKNOWN", 5), 1L)).isEqualTo(0)
        verify(holdInventoryUseCase, never()).hold(any())
    }
}
