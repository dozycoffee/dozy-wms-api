package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.out.InventoryHistoryRepository
import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotFoundException
import com.dozycoffee.wms.inventory.domain.exception.LotNotFoundException
import com.dozycoffee.wms.inventory.application.port.`in`.InventorySortBy
import com.dozycoffee.wms.inventory.domain.model.Inventory
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import com.dozycoffee.wms.inventory.fixture.InventoryHistoryTestBuilder.Companion.inventoryHistory
import com.dozycoffee.wms.inventory.fixture.InventoryTestBuilder.Companion.inventory
import com.dozycoffee.wms.inventory.fixture.LotTestBuilder.Companion.lot
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
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class InventoryServiceTest {

    @Mock
    private lateinit var inventoryRepository: InventoryRepository

    @Mock
    private lateinit var lotRepository: LotRepository

    @Mock
    private lateinit var inventoryHistoryRepository: InventoryHistoryRepository

    @InjectMocks
    private lateinit var inventoryService: InventoryService

    @Nested
    inner class 재고_등록 {

        @Test
        fun `존재하는 Lot을 참조하면 Lot의 상품으로 재고를 등록한다`() = runTest {
            val command = RegisterInventoryCommand(1L, 1L, 10, 100L)
            val existingLot = lot().lotId(1L).productId(7L).build()
            val saved: Inventory = inventory().inventoryId(1L).productId(7L).lotId(1L).build()
            whenever(lotRepository.findById(1L)).thenReturn(existingLot)
            whenever(inventoryRepository.save(any())).thenReturn(saved)
            whenever(inventoryHistoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = inventoryService.register(command)

            assertThat(result.inventoryId).isEqualTo(1L)

            val captor = argumentCaptor<Inventory>()
            verify(inventoryRepository).save(captor.capture())
            assertThat(captor.firstValue.productId).isEqualTo(7L)

            val historyCaptor = argumentCaptor<InventoryHistory>()
            verify(inventoryHistoryRepository).save(historyCaptor.capture())
            assertThat(historyCaptor.firstValue.inventoryId).isEqualTo(1L)
            assertThat(historyCaptor.firstValue.historyType).isEqualTo(InventoryHistoryType.INBOUND)
            assertThat(historyCaptor.firstValue.quantityChange).isEqualTo(10)
            assertThat(historyCaptor.firstValue.referenceId).isEqualTo(100L)
        }

        @Test
        fun `존재하지 않는 Lot을 참조하면 예외를 던진다`() = runTest {
            val command = RegisterInventoryCommand(999L, 1L, 10, 100L)
            whenever(lotRepository.findById(999L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inventoryService.register(command) } }
                .isInstanceOf(LotNotFoundException::class.java)
        }
    }

    @Nested
    inner class 재고_단건_조회 {

        @Test
        fun `존재하는 재고를 조회하면 결과를 반환한다`() = runTest {
            val found: Inventory = inventory().inventoryId(1L).build()
            whenever(inventoryRepository.findById(1L)).thenReturn(found)

            val result = inventoryService.getById(1L)

            assertThat(result.inventoryId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 재고를 조회하면 예외를 던진다`() = runTest {
            whenever(inventoryRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inventoryService.getById(1L) } }
                .isInstanceOf(InventoryNotFoundException::class.java)
        }
    }

    @Nested
    inner class 재고_상세_조회 {

        @Test
        fun `존재하는 재고를 상세 조회하면 Lot과 최근 이력을 함께 반환한다`() = runTest {
            val found: Inventory = inventory().inventoryId(1L).lotId(10L).build()
            val existingLot = lot().lotId(10L).productId(found.productId).build()
            val history = inventoryHistory().inventoryHistoryId(100L).inventoryId(1L).build()
            whenever(inventoryRepository.findById(1L)).thenReturn(found)
            whenever(lotRepository.findById(10L)).thenReturn(existingLot)
            whenever(inventoryHistoryRepository.findRecentByInventoryId(1L, 5)).thenReturn(flowOf(history))

            val result = inventoryService.getDetailById(1L)

            assertThat(result.inventory.inventoryId).isEqualTo(1L)
            assertThat(result.lot.lotId).isEqualTo(10L)
            assertThat(result.recentHistories).hasSize(1)
            assertThat(result.recentHistories[0].inventoryHistoryId).isEqualTo(100L)
        }

        @Test
        fun `존재하지 않는 재고를 상세 조회하면 예외를 던진다`() = runTest {
            whenever(inventoryRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inventoryService.getDetailById(1L) } }
                .isInstanceOf(InventoryNotFoundException::class.java)
        }
    }

    @Nested
    inner class 재고_목록_조회 {

        @Test
        fun `필터 없이 조회하면 전체 재고 목록을 반환한다`() = runTest {
            val found: List<Inventory> = listOf(inventory().inventoryId(1L).build(), inventory().inventoryId(2L).build())
            whenever(inventoryRepository.findAll(null, null, null, null)).thenReturn(flowOf(*found.toTypedArray()))

            val result = inventoryService.getAll(locationId = null, productId = null, qualityStatus = null, sortBy = null).toList()

            assertThat(result).hasSize(2)
        }

        @Test
        fun `정렬 기준을 지정하면 그대로 리포지토리에 전달한다`() = runTest {
            val found: Inventory = inventory().inventoryId(1L).build()
            whenever(inventoryRepository.findAll(null, null, null, InventorySortBy.EXPIRATION_DATE))
                .thenReturn(flowOf(found))

            val result = inventoryService.getAll(null, null, null, InventorySortBy.EXPIRATION_DATE).toList()

            assertThat(result).hasSize(1)
            assertThat(result[0].inventoryId).isEqualTo(1L)
        }
    }

    @Nested
    inner class 재고_불량_처리 {

        @Test
        fun `점유가 없는 정상 재고를 불량 처리하면 품질 상태가 DEFECTIVE로 바뀐다`() = runTest {
            val found: Inventory = inventory().inventoryId(1L).qualityStatus(QualityStatus.NORMAL).build()
            whenever(inventoryRepository.findById(1L)).thenReturn(found)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = inventoryService.markDefective(1L)

            assertThat(result.qualityStatus).isEqualTo(QualityStatus.DEFECTIVE)
        }

        @Test
        fun `존재하지 않는 재고를 불량 처리하면 예외를 던진다`() = runTest {
            whenever(inventoryRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inventoryService.markDefective(1L) } }
                .isInstanceOf(InventoryNotFoundException::class.java)
        }
    }

    @Nested
    inner class 재고_폐기예정_처리 {

        @Test
        fun `점유가 없는 정상 재고를 폐기예정 처리하면 품질 상태가 DISPOSAL_SCHEDULED로 바뀐다`() = runTest {
            val found: Inventory = inventory().inventoryId(1L).qualityStatus(QualityStatus.NORMAL).build()
            whenever(inventoryRepository.findById(1L)).thenReturn(found)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = inventoryService.markDisposalScheduled(1L)

            assertThat(result.qualityStatus).isEqualTo(QualityStatus.DISPOSAL_SCHEDULED)
        }
    }

    @Nested
    inner class 재고_폐기_확정 {

        @Test
        fun `폐기예정 재고를 폐기 확정하면 soft delete된다`() = runTest {
            val found: Inventory =
                inventory().inventoryId(1L).quantity(30).qualityStatus(QualityStatus.DISPOSAL_SCHEDULED).build()
            whenever(inventoryRepository.findById(1L)).thenReturn(found)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryHistoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            inventoryService.confirmDisposal(1L, 200L)

            val captor = argumentCaptor<Inventory>()
            verify(inventoryRepository).save(captor.capture())
            assertThat(captor.firstValue.isDeleted()).isTrue()

            val historyCaptor = argumentCaptor<InventoryHistory>()
            verify(inventoryHistoryRepository).save(historyCaptor.capture())
            assertThat(historyCaptor.firstValue.inventoryId).isEqualTo(1L)
            assertThat(historyCaptor.firstValue.historyType).isEqualTo(InventoryHistoryType.DISPOSAL)
            assertThat(historyCaptor.firstValue.quantityChange).isEqualTo(-30)
            assertThat(historyCaptor.firstValue.referenceId).isEqualTo(200L)
        }

        @Test
        fun `존재하지 않는 재고를 폐기 확정하면 예외를 던진다`() = runTest {
            whenever(inventoryRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inventoryService.confirmDisposal(1L, 200L) } }
                .isInstanceOf(InventoryNotFoundException::class.java)
        }
    }

    @Nested
    inner class 재고_조정 {

        @Test
        fun `실측 수량으로 조정하면 변동분이 ADJUSTMENT 이력으로 기록된다`() = runTest {
            val found: Inventory = inventory().inventoryId(1L).quantity(20).build()
            whenever(inventoryRepository.findById(1L)).thenReturn(found)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryHistoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = inventoryService.adjust(1L, 18, 500L)

            assertThat(result.quantity).isEqualTo(18)
            val historyCaptor = argumentCaptor<InventoryHistory>()
            verify(inventoryHistoryRepository).save(historyCaptor.capture())
            assertThat(historyCaptor.firstValue.historyType).isEqualTo(InventoryHistoryType.ADJUSTMENT)
            assertThat(historyCaptor.firstValue.quantityChange).isEqualTo(-2)
            assertThat(historyCaptor.firstValue.referenceId).isEqualTo(500L)
        }

        @Test
        fun `조정 전후 수량이 같으면 이력을 기록하지 않는다`() = runTest {
            val found: Inventory = inventory().inventoryId(1L).quantity(20).build()
            whenever(inventoryRepository.findById(1L)).thenReturn(found)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            inventoryService.adjust(1L, 20, 500L)

            verify(inventoryHistoryRepository, org.mockito.kotlin.never()).save(any())
        }

        @Test
        fun `존재하지 않는 재고를 조정하면 예외를 던진다`() = runTest {
            whenever(inventoryRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inventoryService.adjust(1L, 18, 500L) } }
                .isInstanceOf(InventoryNotFoundException::class.java)
        }
    }

    @Nested
    inner class 재고_이력_조회 {

        @Test
        fun `기간을 지정하면 시작일 00시부터 종료일 다음날 00시 이전까지로 변환해 조회한다`() = runTest {
            val from = LocalDate.of(2026, 9, 1)
            val to = LocalDate.of(2026, 9, 16)
            val found = inventoryHistory().inventoryHistoryId(1L).build()
            whenever(
                inventoryHistoryRepository.findAll(
                    1L, InventoryHistoryType.INBOUND, from.atStartOfDay(), to.plusDays(1).atStartOfDay()
                )
            ).thenReturn(flowOf(found))

            val result = inventoryService.getAll(1L, InventoryHistoryType.INBOUND, from, to).toList()

            assertThat(result).hasSize(1)
            assertThat(result[0].inventoryHistoryId).isEqualTo(1L)
        }

        @Test
        fun `필터 없이 조회하면 전체 이력을 반환한다`() = runTest {
            val found = listOf(inventoryHistory().inventoryHistoryId(1L).build(), inventoryHistory().inventoryHistoryId(2L).build())
            whenever(inventoryHistoryRepository.findAll(null, null, null, null)).thenReturn(flowOf(*found.toTypedArray()))

            val result = inventoryService.getAll(inventoryId = null, historyType = null, from = null, to = null).toList()

            assertThat(result).hasSize(2)
        }
    }
}
