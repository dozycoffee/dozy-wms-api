package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.Inventory
import com.dozycoffee.wms.inventory.domain.model.Lot
import com.dozycoffee.wms.inventory.fixture.InventoryTestBuilder.Companion.inventory
import com.dozycoffee.wms.inventory.fixture.LotTestBuilder.Companion.lot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ExpirationMonitoringServiceTest {

    @Mock
    private lateinit var lotRepository: LotRepository

    @Mock
    private lateinit var inventoryRepository: InventoryRepository

    @InjectMocks
    private lateinit var expirationMonitoringService: ExpirationMonitoringService

    private val today: LocalDate = LocalDate.now()

    @Nested
    inner class 유통기한_임박_전환 {

        @Test
        fun `정상 상태 Lot의 유통기한이 30일 이내로 다가오면 임박 상태로 전환한다`() = runTest {
            val target: Lot = lot().lotId(1L).lotStatus(LotStatus.NORMAL).expirationDate(today.plusDays(10)).build()
            whenever(lotRepository.findAllByLotStatusNotAndExpirationDateLessThanEqual(eq(LotStatus.EXPIRED), any()))
                .thenReturn(flowOf(target))
            whenever(lotRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = expirationMonitoringService.scan()

            assertThat(result.expiringSoonLotCount).isEqualTo(1)
            assertThat(result.expiredLotCount).isEqualTo(0)
            val captor = argumentCaptor<Lot>()
            verify(lotRepository).save(captor.capture())
            assertThat(captor.firstValue.lotStatus).isEqualTo(LotStatus.EXPIRING_SOON)
        }

        @Test
        fun `이미 임박 상태인 Lot은 다시 전환하지 않는다`() = runTest {
            val target: Lot =
                lot().lotId(1L).lotStatus(LotStatus.EXPIRING_SOON).expirationDate(today.plusDays(10)).build()
            whenever(lotRepository.findAllByLotStatusNotAndExpirationDateLessThanEqual(eq(LotStatus.EXPIRED), any()))
                .thenReturn(flowOf(target))

            val result = expirationMonitoringService.scan()

            assertThat(result.expiringSoonLotCount).isEqualTo(0)
            verify(lotRepository, never()).save(any())
        }
    }

    @Nested
    inner class 유통기한_경과_전환 {

        @Test
        fun `유통기한이 당일 경과한 Lot은 만료 상태로 전환하고 정상 재고를 폐기예정으로 전환한다`() = runTest {
            val expiredLot: Lot = lot().lotId(1L).lotStatus(LotStatus.EXPIRING_SOON).expirationDate(today).build()
            val targetInventory: Inventory =
                inventory().inventoryId(10L).lotId(1L).qualityStatus(QualityStatus.NORMAL).build()
            whenever(lotRepository.findAllByLotStatusNotAndExpirationDateLessThanEqual(eq(LotStatus.EXPIRED), any()))
                .thenReturn(flowOf(expiredLot))
            whenever(lotRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryRepository.findAllByLotId(1L)).thenReturn(flowOf(targetInventory))
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = expirationMonitoringService.scan()

            assertThat(result.expiredLotCount).isEqualTo(1)
            assertThat(result.disposalScheduledInventoryCount).isEqualTo(1)
            assertThat(result.skippedActiveAllocationInventoryCount).isEqualTo(0)

            val lotCaptor = argumentCaptor<Lot>()
            verify(lotRepository).save(lotCaptor.capture())
            assertThat(lotCaptor.firstValue.lotStatus).isEqualTo(LotStatus.EXPIRED)

            val inventoryCaptor = argumentCaptor<Inventory>()
            verify(inventoryRepository).save(inventoryCaptor.capture())
            assertThat(inventoryCaptor.firstValue.qualityStatus).isEqualTo(QualityStatus.DISPOSAL_SCHEDULED)
        }

        @Test
        fun `정상 상태 Lot이 스캔 주기 사이에 유통기한을 완전히 지나쳤어도 한 번에 만료로 전환한다`() = runTest {
            val expiredLot: Lot =
                lot().lotId(1L).lotStatus(LotStatus.NORMAL).expirationDate(today.minusDays(1)).build()
            whenever(lotRepository.findAllByLotStatusNotAndExpirationDateLessThanEqual(eq(LotStatus.EXPIRED), any()))
                .thenReturn(flowOf(expiredLot))
            whenever(lotRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryRepository.findAllByLotId(1L)).thenReturn(emptyFlow())

            val result = expirationMonitoringService.scan()

            assertThat(result.expiredLotCount).isEqualTo(1)
            val captor = argumentCaptor<Lot>()
            verify(lotRepository).save(captor.capture())
            assertThat(captor.firstValue.lotStatus).isEqualTo(LotStatus.EXPIRED)
        }

        @Test
        fun `이미 만료 처리된 Lot은 다시 저장하지 않고 소속 재고만 폐기예정으로 전환한다`() = runTest {
            val expiredLot: Lot = lot().lotId(1L).lotStatus(LotStatus.EXPIRED).expirationDate(today).build()
            val targetInventory: Inventory =
                inventory().inventoryId(10L).lotId(1L).qualityStatus(QualityStatus.NORMAL).build()
            whenever(lotRepository.findAllByLotStatusNotAndExpirationDateLessThanEqual(eq(LotStatus.EXPIRED), any()))
                .thenReturn(flowOf(expiredLot))
            whenever(inventoryRepository.findAllByLotId(1L)).thenReturn(flowOf(targetInventory))
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = expirationMonitoringService.scan()

            assertThat(result.expiredLotCount).isEqualTo(0)
            assertThat(result.disposalScheduledInventoryCount).isEqualTo(1)
            verify(lotRepository, never()).save(any())
        }

        @Test
        fun `이미 정상이 아닌 재고는 건드리지 않는다`() = runTest {
            val expiredLot: Lot = lot().lotId(1L).lotStatus(LotStatus.EXPIRING_SOON).expirationDate(today).build()
            val defectiveInventory: Inventory =
                inventory().inventoryId(10L).lotId(1L).qualityStatus(QualityStatus.DEFECTIVE).build()
            whenever(lotRepository.findAllByLotStatusNotAndExpirationDateLessThanEqual(eq(LotStatus.EXPIRED), any()))
                .thenReturn(flowOf(expiredLot))
            whenever(lotRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryRepository.findAllByLotId(1L)).thenReturn(flowOf(defectiveInventory))

            val result = expirationMonitoringService.scan()

            assertThat(result.disposalScheduledInventoryCount).isEqualTo(0)
            verify(inventoryRepository, never()).save(any())
        }

        @Test
        fun `점유 중인 재고는 강제로 폐기예정 처리하지 않고 건너뛴다`() = runTest {
            val expiredLot: Lot = lot().lotId(1L).lotStatus(LotStatus.EXPIRING_SOON).expirationDate(today).build()
            val heldInventory: Inventory = inventory()
                .inventoryId(10L)
                .lotId(1L)
                .quantity(10)
                .allocatedQuantity(5)
                .qualityStatus(QualityStatus.NORMAL)
                .build()
            whenever(lotRepository.findAllByLotStatusNotAndExpirationDateLessThanEqual(eq(LotStatus.EXPIRED), any()))
                .thenReturn(flowOf(expiredLot))
            whenever(lotRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryRepository.findAllByLotId(1L)).thenReturn(flowOf(heldInventory))

            val result = expirationMonitoringService.scan()

            assertThat(result.disposalScheduledInventoryCount).isEqualTo(0)
            assertThat(result.skippedActiveAllocationInventoryCount).isEqualTo(1)
            assertThat(heldInventory.qualityStatus).isEqualTo(QualityStatus.NORMAL)
            verify(inventoryRepository, never()).save(any())
        }
    }
}
