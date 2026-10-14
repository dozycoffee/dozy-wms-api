package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.outbound.application.port.`in`.command.CreateOutboundFromReservationCommand
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
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
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class OutboundReservationServiceTest {

    @Mock
    private lateinit var outboundRepository: OutboundRepository

    @Mock
    private lateinit var outboundItemRepository: OutboundItemRepository

    @Mock
    private lateinit var outboundAllocationRepository: OutboundAllocationRepository

    @InjectMocks
    private lateinit var service: OutboundReservationService

    private fun command(): CreateOutboundFromReservationCommand =
        CreateOutboundFromReservationCommand(
            warehouseId = 1L,
            reservationId = 7L,
            channel = "OMS",
            externalOrderId = "ORDER-1",
            items = listOf(
                CreateOutboundFromReservationCommand.Item(
                    100L,
                    listOf(
                        CreateOutboundFromReservationCommand.Allocation(55L, "A", LocalDate.of(2027, 1, 1), 4),
                        CreateOutboundFromReservationCommand.Allocation(56L, "B", null, 2)
                    )
                )
            )
        )

    @Test
    fun `예약 확정으로 출고와 상품별 요청 수량, Lot 할당을 만든다`() = runTest {
        whenever(outboundRepository.findByWarehouseIdAndReservationId(1L, 7L)).thenReturn(null)
        whenever(outboundRepository.save(any())).thenReturn(outbound().outboundId(3L).warehouseId(1L).reservationId(7L).build())
        whenever(outboundItemRepository.save(any())).thenReturn(outboundItem().outboundItemId(30L).outboundId(3L).productId(100L).build())
        whenever(outboundAllocationRepository.save(any())).thenAnswer { it.getArgument(0) }

        val created = service.create(command())

        assertThat(created).isTrue()
        val outboundCaptor = argumentCaptor<Outbound>()
        verify(outboundRepository).save(outboundCaptor.capture())
        assertThat(outboundCaptor.firstValue.reservationId).isEqualTo(7L)
        assertThat(outboundCaptor.firstValue.channel).isEqualTo("OMS")
        assertThat(outboundCaptor.firstValue.externalOrderId).isEqualTo("ORDER-1")
        val itemCaptor = argumentCaptor<OutboundItem>()
        verify(outboundItemRepository).save(itemCaptor.capture())
        assertThat(itemCaptor.firstValue.productId).isEqualTo(100L)
        assertThat(itemCaptor.firstValue.requestedQuantity).isEqualTo(6)
        val allocationCaptor = argumentCaptor<OutboundAllocation>()
        verify(outboundAllocationRepository, times(2)).save(allocationCaptor.capture())
        assertThat(allocationCaptor.allValues.map { it.inventoryId }).containsExactly(55L, 56L)
        assertThat(allocationCaptor.allValues.map { it.outboundItemId }).containsOnly(30L)
        assertThat(allocationCaptor.firstValue.lotNumber).isEqualTo("A")
        assertThat(allocationCaptor.firstValue.expirationDate).isEqualTo(LocalDate.of(2027, 1, 1))
    }

    @Test
    fun `같은 창고와 예약의 출고가 이미 있으면 만들지 않는다`() = runTest {
        whenever(outboundRepository.findByWarehouseIdAndReservationId(1L, 7L))
            .thenReturn(outbound().outboundId(3L).warehouseId(1L).reservationId(7L).build())

        val created = service.create(command())

        assertThat(created).isFalse()
        verify(outboundRepository, never()).save(any())
        verify(outboundItemRepository, never()).save(any())
        verify(outboundAllocationRepository, never()).save(any())
    }
}
