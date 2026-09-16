package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.command.HoldInventoryCommand
import com.dozycoffee.wms.inventory.application.port.out.AllocationRepository
import com.dozycoffee.wms.inventory.application.port.out.InventoryHistoryRepository
import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.exception.AllocationNotFoundException
import com.dozycoffee.wms.inventory.domain.model.Allocation
import com.dozycoffee.wms.inventory.domain.model.Inventory
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import com.dozycoffee.wms.inventory.fixture.AllocationTestBuilder.Companion.allocation
import com.dozycoffee.wms.inventory.fixture.InventoryTestBuilder.Companion.inventory
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
import org.springframework.dao.DataIntegrityViolationException

@ExtendWith(MockitoExtension::class)
class AllocationServiceTest {

    @Mock
    private lateinit var allocationRepository: AllocationRepository

    @Mock
    private lateinit var inventoryRepository: InventoryRepository

    @Mock
    private lateinit var inventoryHistoryRepository: InventoryHistoryRepository

    @InjectMocks
    private lateinit var allocationService: AllocationService

    @Nested
    inner class 재고_점유 {

        @Test
        fun `가용 재고가 충분하면 점유를 생성하고 재고의 점유 수량을 늘린다`() = runTest {
            val command = HoldInventoryCommand(1L, AllocationReferenceType.OUTBOUND, 100L, 10)
            val savedAllocation: Allocation = allocation().allocationId(1L).inventoryId(1L).quantity(10).build()
            val targetInventory: Inventory = inventory().inventoryId(1L).quantity(50).allocatedQuantity(0).build()
            whenever(allocationRepository.save(any())).thenReturn(savedAllocation)
            whenever(inventoryRepository.findById(1L)).thenReturn(targetInventory)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = allocationService.hold(command)

            assertThat(result.allocationId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(AllocationStatus.HELD)

            val captor = argumentCaptor<Inventory>()
            verify(inventoryRepository).save(captor.capture())
            assertThat(captor.firstValue.allocatedQuantity).isEqualTo(10)
        }

        @Test
        fun `같은 참조로 중복 점유 요청이 오면 기존 HELD 점유를 그대로 반환하고 재고는 다시 갱신하지 않는다`() = runTest {
            val command = HoldInventoryCommand(1L, AllocationReferenceType.OUTBOUND, 100L, 10)
            val existingAllocation: Allocation =
                allocation().allocationId(1L).inventoryId(1L).referenceId(100L).quantity(10).build()
            whenever(allocationRepository.save(any())).thenThrow(DataIntegrityViolationException("duplicate"))
            whenever(allocationRepository.findHeld(1L, AllocationReferenceType.OUTBOUND, 100L))
                .thenReturn(existingAllocation)

            val result = allocationService.hold(command)

            assertThat(result.allocationId).isEqualTo(1L)
            verify(inventoryRepository, org.mockito.kotlin.never()).findById(any())
        }
    }

    @Nested
    inner class 점유_해제 {

        @Test
        fun `HELD 상태의 점유를 해제하면 재고의 점유 수량이 줄어든다`() = runTest {
            val heldAllocation: Allocation = allocation().allocationId(1L).inventoryId(1L).quantity(10).build()
            val targetInventory: Inventory = inventory().inventoryId(1L).quantity(50).allocatedQuantity(10).build()
            whenever(allocationRepository.findById(1L)).thenReturn(heldAllocation)
            whenever(allocationRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryRepository.findById(1L)).thenReturn(targetInventory)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = allocationService.release(1L)

            assertThat(result.status).isEqualTo(AllocationStatus.RELEASED)

            val captor = argumentCaptor<Inventory>()
            verify(inventoryRepository).save(captor.capture())
            assertThat(captor.firstValue.allocatedQuantity).isEqualTo(0)
        }

        @Test
        fun `존재하지 않는 점유를 해제하면 예외를 던진다`() = runTest {
            whenever(allocationRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { allocationService.release(1L) } }
                .isInstanceOf(AllocationNotFoundException::class.java)
        }
    }

    @Nested
    inner class 점유_이행 {

        @Test
        fun `HELD 상태의 점유를 이행하면 재고의 총 수량과 점유 수량이 함께 줄어든다`() = runTest {
            val heldAllocation: Allocation =
                allocation().allocationId(1L).inventoryId(1L).referenceId(100L).quantity(10).build()
            val targetInventory: Inventory = inventory().inventoryId(1L).quantity(50).allocatedQuantity(10).build()
            whenever(allocationRepository.findById(1L)).thenReturn(heldAllocation)
            whenever(allocationRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryRepository.findById(1L)).thenReturn(targetInventory)
            whenever(inventoryRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inventoryHistoryRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = allocationService.fulfill(1L)

            assertThat(result.status).isEqualTo(AllocationStatus.FULFILLED)

            val captor = argumentCaptor<Inventory>()
            verify(inventoryRepository).save(captor.capture())
            assertThat(captor.firstValue.quantity).isEqualTo(40)
            assertThat(captor.firstValue.allocatedQuantity).isEqualTo(0)

            val historyCaptor = argumentCaptor<InventoryHistory>()
            verify(inventoryHistoryRepository).save(historyCaptor.capture())
            assertThat(historyCaptor.firstValue.inventoryId).isEqualTo(1L)
            assertThat(historyCaptor.firstValue.historyType).isEqualTo(InventoryHistoryType.OUTBOUND)
            assertThat(historyCaptor.firstValue.quantityChange).isEqualTo(-10)
            assertThat(historyCaptor.firstValue.referenceId).isEqualTo(100L)
        }
    }

    @Nested
    inner class 참조_기준_점유_목록_조회 {

        @Test
        fun `참조 주체 기준으로 HELD 상태 점유 목록을 조회한다`() = runTest {
            val heldAllocation: Allocation = allocation().allocationId(1L).referenceId(100L).build()
            whenever(allocationRepository.findAllHeldByReference(AllocationReferenceType.OUTBOUND, 100L))
                .thenReturn(flowOf(heldAllocation))

            val result = allocationService.getAllHeldByReference(AllocationReferenceType.OUTBOUND, 100L).toList()

            assertThat(result).hasSize(1)
            assertThat(result.first().allocationId).isEqualTo(1L)
        }
    }
}
