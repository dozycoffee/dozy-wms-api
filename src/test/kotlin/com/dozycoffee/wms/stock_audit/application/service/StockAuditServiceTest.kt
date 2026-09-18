package com.dozycoffee.wms.stock_audit.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.AdjustInventoryQuantityUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.out.InventoryHistoryRepository
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.RegisterStockAuditCommand
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditItemRepository
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditRepository
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditApprovalRequiredException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditItemsNotFullyCountedException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditNotFoundException
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import com.dozycoffee.wms.stock_audit.fixture.StockAuditItemTestBuilder.Companion.stockAuditItem
import com.dozycoffee.wms.stock_audit.fixture.StockAuditTestBuilder.Companion.stockAudit
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class StockAuditServiceTest {

    @Mock
    private lateinit var stockAuditRepository: StockAuditRepository

    @Mock
    private lateinit var stockAuditItemRepository: StockAuditItemRepository

    @Mock
    private lateinit var getLocationUseCase: GetLocationUseCase

    @Mock
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @Mock
    private lateinit var adjustInventoryQuantityUseCase: AdjustInventoryQuantityUseCase

    @Mock
    private lateinit var inventoryHistoryRepository: InventoryHistoryRepository

    private lateinit var stockAuditService: StockAuditService

    @BeforeEach
    fun setUp() {
        stockAuditService = StockAuditService(
            stockAuditRepository,
            stockAuditItemRepository,
            getLocationUseCase,
            getInventoryUseCase,
            adjustInventoryQuantityUseCase,
            inventoryHistoryRepository,
            ADJUSTMENT_APPROVAL_THRESHOLD
        )
    }

    private fun locationResult(locationId: Long, zoneId: Long): LocationResult {
        return LocationResult(locationId, zoneId, "A-0$locationId", 70, 30, AvailabilityStatus.AVAILABLE)
    }

    private fun inventoryResult(inventoryId: Long, quantity: Int, locationId: Long): InventoryResult {
        return InventoryResult(inventoryId, 100L, 1L, locationId, quantity, 0, quantity, QualityStatus.NORMAL)
    }

    @Nested
    inner class 실사_등록 {

        @Test
        fun `대상 Zone의 모든 Location에 속한 재고를 스냅샷으로 등록한다`() = runTest {
            val command = RegisterStockAuditCommand(1L, 10L)
            val savedAudit: StockAudit = stockAudit().stockAuditId(1L).warehouseId(1L).zoneId(10L).build()
            whenever(stockAuditRepository.save(any())).thenReturn(savedAudit)
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(Flux.just(locationResult(100L, 10L), locationResult(200L, 10L)))
            whenever(getInventoryUseCase.getAll(100L, null, null, null))
                .thenReturn(flowOf(inventoryResult(1L, 20, 100L)))
            whenever(getInventoryUseCase.getAll(200L, null, null, null))
                .thenReturn(flowOf(inventoryResult(2L, 15, 200L)))
            whenever(stockAuditItemRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = stockAuditService.register(command)

            assertThat(result.stockAuditId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(StockAuditStatus.SCHEDULED)
            val captor = argumentCaptor<StockAuditItem>()
            verify(stockAuditItemRepository, org.mockito.kotlin.times(2)).save(captor.capture())
            assertThat(captor.allValues.map { it.inventoryId }).containsExactlyInAnyOrder(1L, 2L)
            assertThat(captor.allValues.map { it.snapshotQuantity }).containsExactlyInAnyOrder(20, 15)
        }
    }

    @Nested
    inner class 담당자_배정 {

        @Test
        fun `존재하는 실사에 담당자를 배정하면 IN_PROGRESS로 전환된다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.SCHEDULED).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = stockAuditService.assign(1L, "담당자A")

            assertThat(result.status).isEqualTo(StockAuditStatus.IN_PROGRESS)
            assertThat(result.assignee).isEqualTo("담당자A")
        }

        @Test
        fun `존재하지 않는 실사에 배정하면 예외를 던진다`() = runTest {
            whenever(stockAuditRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { stockAuditService.assign(1L, "담당자A") } }
                .isInstanceOf(StockAuditNotFoundException::class.java)
        }
    }

    @Nested
    inner class 실사_완료 {

        @Test
        fun `모든 항목이 카운트되면 COMPLETED로 전환된다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.IN_PROGRESS).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(20)
                    .countedQuantity(20).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(inventoryHistoryRepository.findAll(50L, null, item.snapshotTakenAt, null)).thenReturn(flowOf())
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = stockAuditService.complete(1L)

            assertThat(result.status).isEqualTo(StockAuditStatus.COMPLETED)
        }

        @Test
        fun `카운트되지 않은 항목이 있으면 예외를 던진다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.IN_PROGRESS).build()
            val uncountedItem: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).snapshotQuantity(20).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(uncountedItem))

            assertThatThrownBy { runBlocking { stockAuditService.complete(1L) } }
                .isInstanceOf(StockAuditItemsNotFullyCountedException::class.java)
        }

        @Test
        fun `스냅샷 이후 입출고 이력이 있으면 hasUncommittedMovement를 표시한다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.IN_PROGRESS).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(20)
                    .countedQuantity(15).build()
            val history = InventoryHistory.create(50L, InventoryHistoryType.OUTBOUND, -5, 999L)
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(inventoryHistoryRepository.findAll(50L, null, item.snapshotTakenAt, null)).thenReturn(flowOf(history))
            whenever(stockAuditItemRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            stockAuditService.complete(1L)

            val captor = argumentCaptor<StockAuditItem>()
            verify(stockAuditItemRepository).save(captor.capture())
            assertThat(captor.firstValue.hasUncommittedMovement).isTrue()
        }
    }

    @Nested
    inner class 조정_확정 {

        @Test
        fun `조정량이 임계치 이내면 승인자 없이 CLOSED로 전환되고 재고가 조정된다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(20)
                    .countedQuantity(15).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 20, 100L))
            whenever(adjustInventoryQuantityUseCase.adjust(50L, 15, 10L)).thenReturn(inventoryResult(50L, 15, 100L))
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = stockAuditService.close(1L, null)

            assertThat(result.status).isEqualTo(StockAuditStatus.CLOSED)
            assertThat(result.approvedBy).isNull()
            verify(adjustInventoryQuantityUseCase).adjust(50L, 15, 10L)
        }

        @Test
        fun `조정량이 없는 항목은 조정을 호출하지 않는다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(20)
                    .countedQuantity(20).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 20, 100L))
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            stockAuditService.close(1L, null)

            verify(adjustInventoryQuantityUseCase, org.mockito.kotlin.never()).adjust(any(), any(), any())
        }

        @Test
        fun `조정량이 임계치를 초과하는데 승인자가 없으면 예외를 던진다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(30)
                    .countedQuantity(15).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 30, 100L))

            assertThatThrownBy { runBlocking { stockAuditService.close(1L, null) } }
                .isInstanceOf(StockAuditApprovalRequiredException::class.java)
            verify(adjustInventoryQuantityUseCase, org.mockito.kotlin.never()).adjust(any(), any(), any())
        }

        @Test
        fun `조정량이 임계치를 초과해도 승인자가 있으면 CLOSED로 전환되고 재고가 조정된다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(30)
                    .countedQuantity(15).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 30, 100L))
            whenever(adjustInventoryQuantityUseCase.adjust(50L, 15, 10L)).thenReturn(inventoryResult(50L, 15, 100L))
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = stockAuditService.close(1L, "관리자A")

            assertThat(result.status).isEqualTo(StockAuditStatus.CLOSED)
            assertThat(result.approvedBy).isEqualTo("관리자A")
            verify(adjustInventoryQuantityUseCase).adjust(50L, 15, 10L)
        }

        @Test
        fun `존재하지 않는 실사를 마감하면 예외를 던진다`() = runTest {
            whenever(stockAuditRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { stockAuditService.close(1L, null) } }
                .isInstanceOf(StockAuditNotFoundException::class.java)
        }
    }

    companion object {
        private const val ADJUSTMENT_APPROVAL_THRESHOLD = 5
    }
}
