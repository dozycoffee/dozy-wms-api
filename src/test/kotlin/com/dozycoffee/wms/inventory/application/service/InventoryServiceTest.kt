package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotFoundException
import com.dozycoffee.wms.inventory.domain.exception.LotNotFoundException
import com.dozycoffee.wms.inventory.domain.model.Inventory
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

@ExtendWith(MockitoExtension::class)
class InventoryServiceTest {

    @Mock
    private lateinit var inventoryRepository: InventoryRepository

    @Mock
    private lateinit var lotRepository: LotRepository

    @InjectMocks
    private lateinit var inventoryService: InventoryService

    @Nested
    inner class 재고_등록 {

        @Test
        fun `존재하는 Lot을 참조하면 Lot의 상품으로 재고를 등록한다`() = runTest {
            val command = RegisterInventoryCommand(1L, 1L, 10)
            val existingLot = lot().lotId(1L).productId(7L).build()
            val saved: Inventory = inventory().inventoryId(1L).productId(7L).lotId(1L).build()
            whenever(lotRepository.findById(1L)).thenReturn(existingLot)
            whenever(inventoryRepository.save(any())).thenReturn(saved)

            val result = inventoryService.register(command)

            assertThat(result.inventoryId).isEqualTo(1L)

            val captor = argumentCaptor<Inventory>()
            verify(inventoryRepository).save(captor.capture())
            assertThat(captor.firstValue.productId).isEqualTo(7L)
        }

        @Test
        fun `존재하지 않는 Lot을 참조하면 예외를 던진다`() = runTest {
            val command = RegisterInventoryCommand(999L, 1L, 10)
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
    inner class 재고_목록_조회 {

        @Test
        fun `필터 없이 조회하면 전체 재고 목록을 반환한다`() = runTest {
            val found: List<Inventory> = listOf(inventory().inventoryId(1L).build(), inventory().inventoryId(2L).build())
            whenever(inventoryRepository.findAll(null, null, null)).thenReturn(flowOf(*found.toTypedArray()))

            val result = inventoryService.getAll(null, null, null).toList()

            assertThat(result).hasSize(2)
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
            val found: Inventory = inventory().inventoryId(1L).qualityStatus(QualityStatus.DISPOSAL_SCHEDULED).build()
            whenever(inventoryRepository.findById(1L)).thenReturn(found)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            inventoryService.confirmDisposal(1L)

            val captor = argumentCaptor<Inventory>()
            verify(inventoryRepository).save(captor.capture())
            assertThat(captor.firstValue.isDeleted()).isTrue()
        }

        @Test
        fun `존재하지 않는 재고를 폐기 확정하면 예외를 던진다`() = runTest {
            whenever(inventoryRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inventoryService.confirmDisposal(1L) } }
                .isInstanceOf(InventoryNotFoundException::class.java)
        }
    }
}
