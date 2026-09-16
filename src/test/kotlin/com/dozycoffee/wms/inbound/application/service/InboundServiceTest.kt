package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.inbound.application.port.`in`.command.CompleteInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.LotAssignmentCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundItemCommand
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InsufficientZoneCapacityException
import com.dozycoffee.wms.inbound.domain.exception.MissingLotAssignmentException
import com.dozycoffee.wms.inbound.domain.exception.NotAllItemsInspectedException
import com.dozycoffee.wms.inbound.domain.model.Inbound
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
import com.dozycoffee.wms.inbound.fixture.InboundTestBuilder.Companion.inbound
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.emptyFlow
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class InboundServiceTest {

    @Mock
    private lateinit var inboundRepository: InboundRepository

    @Mock
    private lateinit var inboundItemRepository: InboundItemRepository

    @Mock
    private lateinit var getProductUseCase: GetProductUseCase

    @Mock
    private lateinit var getZoneUseCase: GetZoneUseCase

    @Mock
    private lateinit var getLocationUseCase: GetLocationUseCase

    @Mock
    private lateinit var occupyLocationUseCase: OccupyLocationUseCase

    @Mock
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @Mock
    private lateinit var getLotUseCase: GetLotUseCase

    @Mock
    private lateinit var registerLotUseCase: RegisterLotUseCase

    @Mock
    private lateinit var registerInventoryUseCase: RegisterInventoryUseCase

    @Mock
    private lateinit var markInventoryDefectiveUseCase: MarkInventoryDefectiveUseCase

    @Mock
    private lateinit var registerDisposalUseCase: RegisterDisposalUseCase

    @InjectMocks
    private lateinit var inboundService: InboundService

    private fun productResult(productId: Long, category: ProductCategory): ProductResult {
        return ProductResult(productId, "P-$productId", "상품$productId", category, "EA", null, ProductStatus.ACTIVE)
    }

    private fun zoneResult(zoneId: Long, zoneCode: ZoneCode): ZoneResult {
        return ZoneResult(
            zoneId, 1L, zoneCode, zoneCode.zoneName, zoneCode.temperatureType, zoneCode.capacity.value, AvailabilityStatus.AVAILABLE
        )
    }

    private fun locationResult(locationId: Long, zoneId: Long, maxCapacity: Int, usedCapacity: Int): LocationResult {
        return LocationResult(locationId, zoneId, "A-0$locationId", maxCapacity, usedCapacity, AvailabilityStatus.AVAILABLE)
    }

    private fun workAreaResult(usedCapacity: Int): WorkAreaResult {
        return WorkAreaResult(
            1L, 1L, AreaCode.INBOUND, AreaCode.INBOUND.areaName, AreaCode.INBOUND.capacity.value, usedCapacity, AvailabilityStatus.AVAILABLE
        )
    }

    @Nested
    inner class 입고_등록 {

        @Test
        fun `Zone 잔여 capacity가 충분하면 WAITING 상태로 등록되고 입고 상품이 함께 저장된다`() = runTest {
            val command = RegisterInboundCommand(
                warehouseId = 1L,
                expectedArrivalDate = LocalDate.of(2026, 1, 1),
                items = listOf(RegisterInboundItemCommand(productId = 100L, expectedQuantity = 30))
            )
            whenever(getProductUseCase.getById(100L)).thenReturn(productResult(100L, ProductCategory.BEAN))
            whenever(getZoneUseCase.getByWarehouseIdAndZoneCode(1L, ZoneCode.A))
                .thenReturn(Mono.just(zoneResult(10L, ZoneCode.A)))
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(Flux.just(locationResult(1L, 10L, 70, 10)))
            val savedInbound: Inbound = inbound().inboundId(1L).status(InboundStatus.WAITING).build()
            whenever(inboundRepository.save(any())).thenReturn(savedInbound)

            val result = inboundService.register(command)

            assertThat(result.inboundId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(InboundStatus.WAITING)
            verify(inboundItemRepository).save(any())
        }

        @Test
        fun `Zone 잔여 capacity가 부족하면 예외를 던지고 저장하지 않는다`() = runTest {
            val command = RegisterInboundCommand(
                warehouseId = 1L,
                expectedArrivalDate = LocalDate.of(2026, 1, 1),
                items = listOf(RegisterInboundItemCommand(productId = 100L, expectedQuantity = 100))
            )
            whenever(getProductUseCase.getById(100L)).thenReturn(productResult(100L, ProductCategory.BEAN))
            whenever(getZoneUseCase.getByWarehouseIdAndZoneCode(1L, ZoneCode.A))
                .thenReturn(Mono.just(zoneResult(10L, ZoneCode.A)))
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(Flux.just(locationResult(1L, 10L, 70, 10)))

            assertThatThrownBy { runBlocking { inboundService.register(command) } }
                .isInstanceOf(InsufficientZoneCapacityException::class.java)
            verify(inboundRepository, never()).save(any())
        }
    }

    @Nested
    inner class 입고_처리_시작 {

        @Test
        fun `입고 처리장을 점유하고 PROCESSING으로 전환한다`() = runTest {
            val existingInbound: Inbound = inbound().inboundId(1L).warehouseId(1L).status(InboundStatus.WAITING).build()
            whenever(inboundRepository.findById(1L)).thenReturn(existingInbound)
            whenever(inboundItemRepository.findAllByInboundId(1L))
                .thenReturn(flowOf(inboundItem().inboundItemId(1L).inboundId(1L).expectedQuantity(30).build()))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND))
                .thenReturn(Mono.just(workAreaResult(0)))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.just(workAreaResult(30)))
            whenever(inboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = inboundService.startProcessing(1L)

            assertThat(result.status).isEqualTo(InboundStatus.PROCESSING)
            verify(occupyWorkAreaUseCase).occupy(OccupyWorkAreaCommand(1L, 30))
        }

        @Test
        fun `존재하지 않는 입고를 처리 시작하면 예외를 던진다`() = runTest {
            whenever(inboundRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inboundService.startProcessing(1L) } }
                .isInstanceOf(InboundNotFoundException::class.java)
        }
    }

    @Nested
    inner class 입고_완료 {

        @Test
        fun `정상 판정 상품은 Lot을 확정하고 Location에 분산 배치해 재고로 등록한다`() = runTest {
            val existingInbound: Inbound = inbound().inboundId(1L).warehouseId(1L).status(InboundStatus.PROCESSING).build()
            val normalItem: InboundItem = inboundItem()
                .inboundItemId(1L).inboundId(1L).productId(100L).zoneId(10L)
                .expectedQuantity(30).actualQuantity(30).inspectionResult(InspectionResult.NORMAL)
                .build()
            whenever(inboundRepository.findById(1L)).thenReturn(existingInbound)
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(normalItem))
            whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(emptyFlow())
            val registeredLot = LotResult(500L, "LOT-1", 100L, null, null, LotStatus.NORMAL)
            whenever(registerLotUseCase.register(any())).thenReturn(registeredLot)
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(Flux.just(locationResult(1L, 10L, 70, 10)))
            whenever(occupyLocationUseCase.occupy(any())).thenReturn(Mono.just(locationResult(1L, 10L, 70, 40)))
            whenever(registerInventoryUseCase.register(any()))
                .thenReturn(InventoryResult(1L, 100L, 500L, 1L, 30, 0, 30, QualityStatus.NORMAL))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND))
                .thenReturn(Mono.just(workAreaResult(30)))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(workAreaResult(0)))
            whenever(inboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            val command = CompleteInboundCommand(1L, listOf(LotAssignmentCommand(1L, "LOT-1", null, null)))
            val result = inboundService.complete(command)

            assertThat(result.status).isEqualTo(InboundStatus.COMPLETED)
            verify(occupyLocationUseCase).occupy(OccupyLocationCommand(1L, 30))
            verify(registerInventoryUseCase).register(RegisterInventoryCommand(500L, 1L, 30, 1L))
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 30))
        }

        @Test
        fun `검수되지 않은 상품이 있으면 예외를 던진다`() = runTest {
            val existingInbound: Inbound = inbound().inboundId(1L).status(InboundStatus.PROCESSING).build()
            val pendingItem: InboundItem = inboundItem().inboundItemId(1L).inboundId(1L).build()
            whenever(inboundRepository.findById(1L)).thenReturn(existingInbound)
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(pendingItem))

            assertThatThrownBy {
                runBlocking { inboundService.complete(CompleteInboundCommand(1L, emptyList())) }
            }.isInstanceOf(NotAllItemsInspectedException::class.java)
        }

        @Test
        fun `정상 판정 상품에 Lot 정보가 없으면 예외를 던진다`() = runTest {
            val existingInbound: Inbound = inbound().inboundId(1L).status(InboundStatus.PROCESSING).build()
            val normalItem: InboundItem = inboundItem()
                .inboundItemId(1L).inboundId(1L).actualQuantity(10).inspectionResult(InspectionResult.NORMAL).build()
            whenever(inboundRepository.findById(1L)).thenReturn(existingInbound)
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(normalItem))

            assertThatThrownBy {
                runBlocking { inboundService.complete(CompleteInboundCommand(1L, emptyList())) }
            }.isInstanceOf(MissingLotAssignmentException::class.java)
        }

        @Test
        fun `불량 판정 상품은 DEFECTIVE Inventory로 등록되고 검수 불량 사유로 폐기 등록과 연계된다`() = runTest {
            val existingInbound: Inbound = inbound().inboundId(1L).warehouseId(1L).status(InboundStatus.PROCESSING).build()
            val defectiveItem: InboundItem = inboundItem()
                .inboundItemId(1L).inboundId(1L).productId(100L).zoneId(10L)
                .expectedQuantity(30).actualQuantity(30).inspectionResult(InspectionResult.DEFECTIVE)
                .build()
            whenever(inboundRepository.findById(1L)).thenReturn(existingInbound)
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(defectiveItem))
            whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(emptyFlow())
            val registeredLot = LotResult(500L, "LOT-1", 100L, null, null, LotStatus.NORMAL)
            whenever(registerLotUseCase.register(any())).thenReturn(registeredLot)
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(Flux.just(locationResult(1L, 10L, 70, 10)))
            whenever(occupyLocationUseCase.occupy(any())).thenReturn(Mono.just(locationResult(1L, 10L, 70, 40)))
            val registeredInventory = InventoryResult(1L, 100L, 500L, 1L, 30, 0, 30, QualityStatus.NORMAL)
            whenever(registerInventoryUseCase.register(any())).thenReturn(registeredInventory)
            whenever(markInventoryDefectiveUseCase.markDefective(1L))
                .thenReturn(registeredInventory.copy(qualityStatus = QualityStatus.DEFECTIVE))
            whenever(registerDisposalUseCase.register(any()))
                .thenReturn(DisposalResult(900L, 1L, DisposalStatus.REQUESTED))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND))
                .thenReturn(Mono.just(workAreaResult(30)))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(workAreaResult(0)))
            whenever(inboundRepository.save(any())).thenAnswer { it.getArgument(0) }

            val command = CompleteInboundCommand(1L, listOf(LotAssignmentCommand(1L, "LOT-1", null, null)))
            val result = inboundService.complete(command)

            assertThat(result.status).isEqualTo(InboundStatus.COMPLETED)
            verify(markInventoryDefectiveUseCase).markDefective(1L)
            verify(registerDisposalUseCase).register(
                RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(1L, 30, DisposalReason.INSPECTION_DEFECT)))
            )
        }
    }
}
