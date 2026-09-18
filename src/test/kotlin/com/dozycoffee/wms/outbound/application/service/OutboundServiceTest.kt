package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.FulfillAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetAllocationUseCase
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
import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundCommand
import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundItemCommand
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class OutboundServiceTest {

    @Mock
    private lateinit var outboundRepository: OutboundRepository

    @Mock
    private lateinit var outboundItemRepository: OutboundItemRepository

    @Mock
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @Mock
    private lateinit var getLotUseCase: GetLotUseCase

    @Mock
    private lateinit var holdInventoryUseCase: HoldInventoryUseCase

    @Mock
    private lateinit var getAllocationUseCase: GetAllocationUseCase

    @Mock
    private lateinit var fulfillAllocationUseCase: FulfillAllocationUseCase

    @Mock
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @Mock
    private lateinit var releaseLocationUseCase: ReleaseLocationUseCase

    @InjectMocks
    private lateinit var outboundService: OutboundService

    private fun workAreaResult(usedCapacity: Int): WorkAreaResult {
        return WorkAreaResult(
            1L, 1L, AreaCode.OUTBOUND, AreaCode.OUTBOUND.areaName, AreaCode.OUTBOUND.capacity.value, usedCapacity, AvailabilityStatus.AVAILABLE
        )
    }

    private fun locationResult(locationId: Long): LocationResult {
        return LocationResult(locationId, 10L, "A-0$locationId", 70, 30, AvailabilityStatus.AVAILABLE)
    }

    private fun inventoryResult(inventoryId: Long, lotId: Long, locationId: Long, availableQuantity: Int): InventoryResult {
        return InventoryResult(inventoryId, 100L, lotId, locationId, availableQuantity, 0, availableQuantity, QualityStatus.NORMAL)
    }

    private fun lotResult(lotId: Long, expirationDate: LocalDate?): LotResult {
        return LotResult(lotId, "LOT-$lotId", 100L, null, expirationDate, LotStatus.NORMAL)
    }

    private fun allocationResult(allocationId: Long, inventoryId: Long, quantity: Int): AllocationResult {
        return AllocationResult(allocationId, inventoryId, AllocationReferenceType.OUTBOUND, 1L, quantity, AllocationStatus.HELD)
    }

    @Nested
    inner class 출고_등록 {

        @Test
        fun `출고를 REQUESTED 상태로 등록하고 출고 상품이 함께 저장된다`() = runTest {
            val command = RegisterOutboundCommand(1L, listOf(RegisterOutboundItemCommand(100L, 15)))
            val savedOutbound: Outbound = outbound().outboundId(1L).build()
            whenever(outboundRepository.save(any())).thenReturn(savedOutbound)

            val result = outboundService.register(command)

            assertThat(result.outboundId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(OutboundStatus.REQUESTED)
            verify(outboundItemRepository).save(any())
        }
    }

    @Nested
    inner class 피킹_시작 {

        @Test
        fun `유통기한이 이른 Lot부터 FIFO로 피킹하고 출고장을 점유한다`() = runTest {
            val existingOutbound: Outbound = outbound().outboundId(1L).warehouseId(1L).status(OutboundStatus.REQUESTED).build()
            val item: OutboundItem = outboundItem().outboundItemId(1L).outboundId(1L).productId(100L).requestedQuantity(15).build()
            whenever(outboundRepository.findById(1L)).thenReturn(existingOutbound)
            whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item))
            whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(
                flowOf(
                    lotResult(20L, LocalDate.of(2026, 2, 1)),
                    lotResult(10L, LocalDate.of(2026, 1, 1))
                )
            )
            whenever(getInventoryUseCase.getAll(null, 100L, QualityStatus.NORMAL, null)).thenReturn(
                flowOf(
                    inventoryResult(2L, 20L, 200L, 20),
                    inventoryResult(1L, 10L, 100L, 5)
                )
            )
            whenever(holdInventoryUseCase.hold(any())).thenReturn(allocationResult(900L, 1L, 5))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.OUTBOUND)).thenReturn(Mono.just(workAreaResult(0)))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.just(workAreaResult(15)))
            whenever(outboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = outboundService.startPicking(1L)

            assertThat(result.status).isEqualTo(OutboundStatus.PICKING)
            val holdCaptor = argumentCaptor<HoldInventoryCommand>()
            verify(holdInventoryUseCase, times(2)).hold(holdCaptor.capture())
            assertThat(holdCaptor.firstValue).isEqualTo(HoldInventoryCommand(1L, AllocationReferenceType.OUTBOUND, 1L, 5))
            assertThat(holdCaptor.secondValue).isEqualTo(HoldInventoryCommand(2L, AllocationReferenceType.OUTBOUND, 1L, 10))
            verify(occupyWorkAreaUseCase).occupy(OccupyWorkAreaCommand(1L, 15))

            val itemCaptor = argumentCaptor<OutboundItem>()
            verify(outboundItemRepository).save(itemCaptor.capture())
            assertThat(itemCaptor.firstValue.pickedQuantity).isEqualTo(15)
        }

        @Test
        fun `가용 재고가 부족하면 확보 가능한 만큼만 피킹한다`() = runTest {
            val existingOutbound: Outbound = outbound().outboundId(1L).warehouseId(1L).status(OutboundStatus.REQUESTED).build()
            val item: OutboundItem = outboundItem().outboundItemId(1L).outboundId(1L).productId(100L).requestedQuantity(15).build()
            whenever(outboundRepository.findById(1L)).thenReturn(existingOutbound)
            whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item))
            whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(flowOf(lotResult(10L, LocalDate.of(2026, 1, 1))))
            whenever(getInventoryUseCase.getAll(null, 100L, QualityStatus.NORMAL, null)).thenReturn(flowOf(inventoryResult(1L, 10L, 100L, 10)))
            whenever(holdInventoryUseCase.hold(any())).thenReturn(allocationResult(900L, 1L, 10))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.OUTBOUND)).thenReturn(Mono.just(workAreaResult(0)))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.just(workAreaResult(10)))
            whenever(outboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            outboundService.startPicking(1L)

            val itemCaptor = argumentCaptor<OutboundItem>()
            verify(outboundItemRepository).save(itemCaptor.capture())
            assertThat(itemCaptor.firstValue.pickedQuantity).isEqualTo(10)
            assertThat(itemCaptor.firstValue.shortageQuantity).isEqualTo(5)
            verify(occupyWorkAreaUseCase).occupy(OccupyWorkAreaCommand(1L, 10))
        }

        @Test
        fun `존재하지 않는 출고를 피킹 시작하면 예외를 던진다`() = runTest {
            whenever(outboundRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { outboundService.startPicking(1L) } }
                .isInstanceOf(OutboundNotFoundException::class.java)
        }
    }

    @Nested
    inner class 검수_시작 {

        @Test
        fun `INSPECTING으로 전환한다`() = runTest {
            val existingOutbound: Outbound = outbound().outboundId(1L).status(OutboundStatus.PICKING).build()
            whenever(outboundRepository.findById(1L)).thenReturn(existingOutbound)
            whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = outboundService.startInspecting(1L)

            assertThat(result.status).isEqualTo(OutboundStatus.INSPECTING)
        }
    }

    @Nested
    inner class 출고_완료 {

        @Test
        fun `점유를 이행 확정하고 Location과 출고장 점유를 해제한다`() = runTest {
            val existingOutbound: Outbound = outbound().outboundId(1L).warehouseId(1L).status(OutboundStatus.INSPECTING).build()
            val item: OutboundItem = outboundItem()
                .outboundItemId(1L).outboundId(1L).productId(100L).requestedQuantity(15).pickedQuantity(15).build()
            whenever(outboundRepository.findById(1L)).thenReturn(existingOutbound)
            whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item))
            whenever(getAllocationUseCase.getAllHeldByReference(AllocationReferenceType.OUTBOUND, 1L)).thenReturn(
                flowOf(allocationResult(901L, 1L, 5), allocationResult(902L, 2L, 10))
            )
            whenever(getInventoryUseCase.getById(1L)).thenReturn(inventoryResult(1L, 10L, 100L, 0))
            whenever(getInventoryUseCase.getById(2L)).thenReturn(inventoryResult(2L, 20L, 200L, 0))
            whenever(fulfillAllocationUseCase.fulfill(any())).thenReturn(allocationResult(901L, 1L, 5))
            whenever(releaseLocationUseCase.release(any())).thenReturn(Mono.just(locationResult(100L)))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.OUTBOUND)).thenReturn(Mono.just(workAreaResult(15)))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(workAreaResult(0)))
            whenever(outboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = outboundService.complete(1L)

            assertThat(result.status).isEqualTo(OutboundStatus.COMPLETED)
            verify(fulfillAllocationUseCase).fulfill(901L)
            verify(fulfillAllocationUseCase).fulfill(902L)
            verify(releaseLocationUseCase).release(ReleaseLocationCommand(100L, 5))
            verify(releaseLocationUseCase).release(ReleaseLocationCommand(200L, 10))
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 15))
        }

        @Test
        fun `존재하지 않는 출고를 완료하면 예외를 던진다`() = runTest {
            whenever(outboundRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { outboundService.complete(1L) } }
                .isInstanceOf(OutboundNotFoundException::class.java)
        }
    }

    @Nested
    inner class 단건_조회 {

        @Test
        fun `존재하지 않는 출고를 조회하면 예외를 던진다`() = runTest {
            whenever(outboundRepository.findById(999L)).thenReturn(null)

            assertThatThrownBy { runBlocking { outboundService.getById(999L) } }
                .isInstanceOf(OutboundNotFoundException::class.java)
        }
    }
}
