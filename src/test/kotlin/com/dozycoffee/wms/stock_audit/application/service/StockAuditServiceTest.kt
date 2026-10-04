package com.dozycoffee.wms.stock_audit.application.service

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.global.security.Actor
import com.dozycoffee.wms.global.security.CurrentActorProvider
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.SystemActor
import com.dozycoffee.wms.global.security.UserActor
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.inventory.application.port.`in`.AdjustInventoryQuantityUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.out.InventoryHistoryRepository
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.RegisterStockAuditCommand
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditItemRepository
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditRepository
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditApprovalRequiredException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditZoneWarehouseMismatchException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditItemsNotFullyCountedException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditNotFoundException
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import com.dozycoffee.wms.stock_audit.fixture.StockAuditItemTestBuilder.Companion.stockAuditItem
import com.dozycoffee.wms.stock_audit.fixture.StockAuditTestBuilder.Companion.stockAudit
import com.dozycoffee.wms.support.SwitchableWarehouseAccess
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.LocationCapacityExceededException
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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

@ExtendWith(MockitoExtension::class)
class StockAuditServiceTest {

    @Mock
    private lateinit var stockAuditRepository: StockAuditRepository

    @Mock
    private lateinit var stockAuditItemRepository: StockAuditItemRepository

    @Mock
    private lateinit var getZoneUseCase: GetZoneUseCase

    @Mock
    private lateinit var getLocationUseCase: GetLocationUseCase

    @Mock
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @Mock
    private lateinit var adjustInventoryQuantityUseCase: AdjustInventoryQuantityUseCase

    @Mock
    private lateinit var occupyLocationUseCase: OccupyLocationUseCase

    @Mock
    private lateinit var releaseLocationUseCase: ReleaseLocationUseCase

    @Mock
    private lateinit var inventoryHistoryRepository: InventoryHistoryRepository

    private val warehouseAccess = SwitchableWarehouseAccess()

    private var actor: Actor = SystemActor

    private val currentActorProvider: CurrentActorProvider = object : CurrentActorProvider {
        override suspend fun get(): Actor = actor
    }

    private lateinit var stockAuditService: StockAuditService

    @BeforeEach
    fun setUp() {
        stockAuditService = StockAuditService(
            WarehouseAccessGuard(warehouseAccess),
            stockAuditRepository,
            stockAuditItemRepository,
            getZoneUseCase,
            getLocationUseCase,
            getInventoryUseCase,
            adjustInventoryQuantityUseCase,
            occupyLocationUseCase,
            releaseLocationUseCase,
            inventoryHistoryRepository,
            currentActorProvider,
            ADJUSTMENT_APPROVAL_THRESHOLD
        )
    }

    private fun locationResult(locationId: Long, zoneId: Long): LocationResult {
        return LocationResult(locationId, zoneId, "A-0$locationId", 70, 30, AvailabilityStatus.AVAILABLE)
    }

    private fun zoneResult(zoneId: Long, warehouseId: Long): ZoneResult {
        return ZoneResult(zoneId, warehouseId, ZoneCode.A, "A Zone", TemperatureType.AMBIENT, 820, AvailabilityStatus.AVAILABLE)
    }

    private fun inventoryResult(inventoryId: Long, quantity: Int, locationId: Long): InventoryResult {
        return InventoryResult(inventoryId, 100L, 1L, locationId, quantity, 0, quantity, QualityStatus.NORMAL)
    }

    @Nested
    inner class 실사_등록 {

        @Test
        fun `Zone이 요청한 창고에 속하지 않으면 예외를 던지고 저장하지 않는다`() = runTest {
            whenever(getZoneUseCase.getById(10L)).thenReturn(zoneResult(10L, 2L))

            assertThatThrownBy { runBlocking { stockAuditService.register(RegisterStockAuditCommand(1L, 10L)) } }
                .isInstanceOf(StockAuditZoneWarehouseMismatchException::class.java)
            verify(stockAuditRepository, org.mockito.kotlin.never()).save(any())
        }

        @Test
        fun `대상 Zone의 모든 Location에 속한 재고를 스냅샷으로 등록한다`() = runTest {
            val command = RegisterStockAuditCommand(1L, 10L)
            whenever(getZoneUseCase.getById(10L)).thenReturn(zoneResult(10L, 1L))
            val savedAudit: StockAudit = stockAudit().stockAuditId(1L).warehouseId(1L).zoneId(10L).build()
            whenever(stockAuditRepository.save(any())).thenReturn(savedAudit)
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(flowOf(locationResult(100L, 10L), locationResult(200L, 10L)))
            whenever(getInventoryUseCase.getAll(100L, null, null, null, null))
                .thenReturn(flowOf(inventoryResult(1L, 20, 100L)))
            whenever(getInventoryUseCase.getAll(200L, null, null, null, null))
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
            actor = UserActor(ADMIN_ID, setOf("stock_audit_manager"))

            val result = stockAuditService.assign(1L)

            assertThat(result.status).isEqualTo(StockAuditStatus.IN_PROGRESS)
            assertThat(result.assignee).isEqualTo(ADMIN_ID.toString())
        }

        @Test
        fun `사용자가 아닌 행위자가 배정하면 담당자 예외를 던진다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.SCHEDULED).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            actor = SystemActor

            assertThatThrownBy { runBlocking { stockAuditService.assign(1L) } }
                .isInstanceOf(InvalidDomainValueException::class.java)
        }

        @Test
        fun `존재하지 않는 실사에 배정하면 예외를 던진다`() = runTest {
            whenever(stockAuditRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { stockAuditService.assign(1L) } }
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
            whenever(releaseLocationUseCase.release(any())).thenReturn(locationResult(100L, 1L))
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = stockAuditService.close(1L)

            assertThat(result.status).isEqualTo(StockAuditStatus.CLOSED)
            assertThat(result.approvedBy).isNull()
            verify(adjustInventoryQuantityUseCase).adjust(50L, 15, 10L)
        }

        @Test
        fun `실측이 시스템 수량보다 적으면 부족분만큼 Location 사용량을 해제한다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(20)
                    .countedQuantity(15).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 20, 100L))
            whenever(adjustInventoryQuantityUseCase.adjust(50L, 15, 10L)).thenReturn(inventoryResult(50L, 15, 100L))
            whenever(releaseLocationUseCase.release(any())).thenReturn(locationResult(100L, 1L))
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            stockAuditService.close(1L)

            verify(releaseLocationUseCase).release(ReleaseLocationCommand(100L, 5))
            verify(occupyLocationUseCase, org.mockito.kotlin.never()).occupy(any())
        }

        @Test
        fun `실측이 시스템 수량보다 많으면 초과분만큼 Location 사용량을 점유한다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(20)
                    .countedQuantity(24).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 20, 100L))
            whenever(adjustInventoryQuantityUseCase.adjust(50L, 24, 10L)).thenReturn(inventoryResult(50L, 24, 100L))
            whenever(occupyLocationUseCase.occupy(any())).thenReturn(locationResult(100L, 1L))
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            stockAuditService.close(1L)

            verify(occupyLocationUseCase).occupy(OccupyLocationCommand(100L, 4))
            verify(releaseLocationUseCase, org.mockito.kotlin.never()).release(any())
        }

        @Test
        fun `초과분이 Location 최대 용량을 넘으면 예외가 전파된다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(20)
                    .countedQuantity(24).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 20, 100L))
            whenever(adjustInventoryQuantityUseCase.adjust(50L, 24, 10L)).thenReturn(inventoryResult(50L, 24, 100L))
            whenever(occupyLocationUseCase.occupy(any())).thenThrow(LocationCapacityExceededException())

            assertThatThrownBy { runBlocking { stockAuditService.close(1L) } }
                .isInstanceOf(LocationCapacityExceededException::class.java)
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

            stockAuditService.close(1L)

            verify(adjustInventoryQuantityUseCase, org.mockito.kotlin.never()).adjust(any(), any(), any())
            verify(occupyLocationUseCase, org.mockito.kotlin.never()).occupy(any())
            verify(releaseLocationUseCase, org.mockito.kotlin.never()).release(any())
        }

        @Test
        fun `조정량이 임계치를 초과하는데 시스템 행위자가 마감하면 승인 필요 예외를 던진다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(30)
                    .countedQuantity(15).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 30, 100L))

            assertThatThrownBy { runBlocking { stockAuditService.close(1L) } }
                .isInstanceOf(StockAuditApprovalRequiredException::class.java)
            verify(adjustInventoryQuantityUseCase, org.mockito.kotlin.never()).adjust(any(), any(), any())
        }

        @Test
        fun `조정량이 임계치를 초과해도 warehouse_admin이 마감하면 승인자로 기록되고 CLOSED로 전환되고 재고가 조정된다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(30)
                    .countedQuantity(15).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 30, 100L))
            whenever(adjustInventoryQuantityUseCase.adjust(50L, 15, 10L)).thenReturn(inventoryResult(50L, 15, 100L))
            whenever(releaseLocationUseCase.release(any())).thenReturn(locationResult(100L, 1L))
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }

            actor = UserActor(ADMIN_ID, setOf("warehouse_admin"))

            val result = stockAuditService.close(1L)

            assertThat(result.status).isEqualTo(StockAuditStatus.CLOSED)
            assertThat(result.approvedBy).isEqualTo(ADMIN_ID.toString())
            verify(adjustInventoryQuantityUseCase).adjust(50L, 15, 10L)
        }

        @Test
        fun `조정량이 임계치를 초과하는데 warehouse_admin이 아닌 담당자가 마감하면 승인 필요 예외를 던진다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(30)
                    .countedQuantity(15).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 30, 100L))
            actor = UserActor(ADMIN_ID, setOf("stock_audit_manager"))

            assertThatThrownBy { runBlocking { stockAuditService.close(1L) } }
                .isInstanceOf(StockAuditApprovalRequiredException::class.java)
            verify(adjustInventoryQuantityUseCase, org.mockito.kotlin.never()).adjust(any(), any(), any())
        }

        @Test
        fun `조정량이 임계치 이내면 warehouse_admin이 마감해도 승인자를 기록하지 않는다`() = runTest {
            val existing: StockAudit = stockAudit().stockAuditId(1L).status(StockAuditStatus.COMPLETED).build()
            val item: StockAuditItem =
                stockAuditItem().stockAuditItemId(10L).stockAuditId(1L).inventoryId(50L).snapshotQuantity(20)
                    .countedQuantity(15).build()
            whenever(stockAuditRepository.findById(1L)).thenReturn(existing)
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(50L)).thenReturn(inventoryResult(50L, 20, 100L))
            whenever(adjustInventoryQuantityUseCase.adjust(50L, 15, 10L)).thenReturn(inventoryResult(50L, 15, 100L))
            whenever(releaseLocationUseCase.release(any())).thenReturn(locationResult(100L, 1L))
            whenever(stockAuditRepository.save(any())).thenAnswer { it.getArgument(0) }
            actor = UserActor(ADMIN_ID, setOf("warehouse_admin"))

            val result = stockAuditService.close(1L)

            assertThat(result.approvedBy).isNull()
        }

        @Test
        fun `존재하지 않는 실사를 마감하면 예외를 던진다`() = runTest {
            whenever(stockAuditRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { stockAuditService.close(1L) } }
                .isInstanceOf(StockAuditNotFoundException::class.java)
        }
    }

    companion object {
        private const val ADJUSTMENT_APPROVAL_THRESHOLD = 5
        private val ADMIN_ID: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")
    }

    @Nested
    inner class 창고_접근 {

        @Test
        fun `접근할 수 없는 창고에는 실사를 등록할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))

            assertThatThrownBy { runBlocking { stockAuditService.register(RegisterStockAuditCommand(1L, 10L)) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(stockAuditRepository, org.mockito.kotlin.never()).save(any())
        }

        @Test
        fun `접근할 수 없는 창고의 실사는 단건 조회할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))
            whenever(stockAuditRepository.findById(1L)).thenReturn(stockAudit().stockAuditId(1L).warehouseId(1L).build())

            assertThatThrownBy { runBlocking { stockAuditService.getById(1L) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
        }

        @Test
        fun `접근할 수 없는 창고의 실사는 마감할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))
            whenever(stockAuditRepository.findById(1L)).thenReturn(stockAudit().stockAuditId(1L).warehouseId(1L).build())

            assertThatThrownBy { runBlocking { stockAuditService.close(1L) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(adjustInventoryQuantityUseCase, org.mockito.kotlin.never()).adjust(any(), any(), any())
        }

        @Test
        fun `요청한 창고가 접근 범위 밖이면 저장소를 조회하지 않고 빈 목록을 반환한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))

            val result = stockAuditService.getAll(1L, null).toList()

            assertThat(result).isEmpty()
            verify(stockAuditRepository, org.mockito.kotlin.never()).findAll(any(), any())
        }

        @Test
        fun `창고를 지정하지 않으면 접근 가능한 창고로 좁혀 조회한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(1L, 3L))
            whenever(stockAuditRepository.findAll(listOf(1L, 3L), null)).thenReturn(flowOf())

            stockAuditService.getAll(null, null).toList()

            verify(stockAuditRepository).findAll(listOf(1L, 3L), null)
        }
    }
}
