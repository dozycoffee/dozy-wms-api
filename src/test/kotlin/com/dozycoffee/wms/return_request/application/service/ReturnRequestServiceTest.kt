package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
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
import com.dozycoffee.wms.return_request.application.port.`in`.command.CompleteReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnItemCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.ReturnItemLotAssignmentCommand
import com.dozycoffee.wms.return_request.application.port.out.ReturnItemRepository
import com.dozycoffee.wms.return_request.application.port.out.ReturnRequestRepository
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.exception.InsufficientZoneCapacityException
import com.dozycoffee.wms.return_request.domain.exception.MissingLotAssignmentException
import com.dozycoffee.wms.return_request.domain.exception.NotAllReturnItemsInspectedException
import com.dozycoffee.wms.return_request.domain.exception.ReturnRequestNotFoundException
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import com.dozycoffee.wms.return_request.fixture.ReturnItemTestBuilder.Companion.returnItem
import com.dozycoffee.wms.return_request.fixture.ReturnRequestTestBuilder.Companion.returnRequest
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
class ReturnRequestServiceTest {

    @Mock
    private lateinit var returnRequestRepository: ReturnRequestRepository

    @Mock
    private lateinit var returnItemRepository: ReturnItemRepository

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
    private lateinit var returnRequestService: ReturnRequestService

    private fun workAreaResult(usedCapacity: Int): WorkAreaResult {
        return WorkAreaResult(
            1L, 1L, AreaCode.RETURN, AreaCode.RETURN.areaName, AreaCode.RETURN.capacity.value, usedCapacity, AvailabilityStatus.AVAILABLE
        )
    }

    private fun productResult(productId: Long, category: ProductCategory = ProductCategory.BEAN): ProductResult {
        return ProductResult(productId, "PRD-$productId", "상품$productId", category, "kg", 365, ProductStatus.ACTIVE)
    }

    private fun zoneResult(zoneId: Long, zoneCode: ZoneCode): ZoneResult {
        return ZoneResult(
            zoneId, 1L, zoneCode, zoneCode.zoneName, zoneCode.temperatureType, zoneCode.capacity.value, AvailabilityStatus.AVAILABLE
        )
    }

    private fun locationResult(locationId: Long, zoneId: Long, maxCapacity: Int, usedCapacity: Int): LocationResult {
        return LocationResult(locationId, zoneId, "A-0$locationId", maxCapacity, usedCapacity, AvailabilityStatus.AVAILABLE)
    }

    @Nested
    inner class 반품_등록 {

        @Test
        fun `유효한 정보를 입력하면 RECEIVED 상태로 등록된다`() = runTest {
            val command = RegisterReturnRequestCommand(1L, listOf(RegisterReturnItemCommand(10L, 5)))
            val savedReturnRequest: ReturnRequest = returnRequest().returnRequestId(1L).build()
            whenever(returnRequestRepository.save(any())).thenReturn(savedReturnRequest)
            whenever(getProductUseCase.getById(10L)).thenReturn(productResult(10L))

            val result = returnRequestService.register(command)

            assertThat(result.returnRequestId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(ReturnRequestStatus.RECEIVED)
            verify(returnItemRepository).save(any())
        }
    }

    @Nested
    inner class 검수_시작 {

        @Test
        fun `반품 신고 수량만큼 반품 처리장을 점유하고 INSPECTING으로 전환한다`() = runTest {
            val existingReturnRequest: ReturnRequest =
                returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.RECEIVED).build()
            val item: ReturnItem = returnItem().returnItemId(1L).returnRequestId(1L).expectedQuantity(5).build()
            whenever(returnRequestRepository.findById(1L)).thenReturn(existingReturnRequest)
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.RETURN)).thenReturn(Mono.just(workAreaResult(0)))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.just(workAreaResult(5)))
            whenever(returnRequestRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = returnRequestService.startInspecting(1L)

            assertThat(result.status).isEqualTo(ReturnRequestStatus.INSPECTING)
            verify(occupyWorkAreaUseCase).occupy(OccupyWorkAreaCommand(1L, 5))
        }

        @Test
        fun `존재하지 않는 반품의 검수를 시작하면 예외를 던진다`() = runTest {
            whenever(returnRequestRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { returnRequestService.startInspecting(1L) } }
                .isInstanceOf(ReturnRequestNotFoundException::class.java)
        }
    }

    @Nested
    inner class 반품_완료 {

        @Test
        fun `정상 판정 상품은 Lot을 확정하고 Location에 분산 배치해 재고로 복귀시킨다`() = runTest {
            val existingReturnRequest: ReturnRequest =
                returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.INSPECTING).build()
            val item: ReturnItem = returnItem()
                .returnItemId(1L)
                .returnRequestId(1L)
                .productId(100L)
                .expectedQuantity(5)
                .actualQuantity(5)
                .inspectionResult(ReturnInspectionResult.NORMAL)
                .build()
            whenever(returnRequestRepository.findById(1L)).thenReturn(existingReturnRequest)
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))
            whenever(getProductUseCase.getById(100L)).thenReturn(productResult(100L))
            whenever(getZoneUseCase.getByWarehouseIdAndZoneCode(1L, ZoneCode.A))
                .thenReturn(Mono.just(zoneResult(10L, ZoneCode.A)))
            whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(emptyFlow())
            val registeredLot = LotResult(500L, "LOT-1", 100L, null, null, LotStatus.NORMAL)
            whenever(registerLotUseCase.register(any())).thenReturn(registeredLot)
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(Flux.just(locationResult(1L, 10L, 70, 10)))
            whenever(occupyLocationUseCase.occupy(any())).thenReturn(Mono.just(locationResult(1L, 10L, 70, 15)))
            whenever(registerInventoryUseCase.register(any()))
                .thenReturn(InventoryResult(1L, 100L, 500L, 1L, 5, 0, 5, QualityStatus.NORMAL))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.RETURN)).thenReturn(Mono.just(workAreaResult(5)))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(workAreaResult(0)))
            whenever(returnRequestRepository.save(any())).thenAnswer { it.getArgument(0) }

            val command = CompleteReturnRequestCommand(1L, listOf(ReturnItemLotAssignmentCommand(1L, "LOT-1", null, null)))
            val result = returnRequestService.complete(command)

            assertThat(result.status).isEqualTo(ReturnRequestStatus.COMPLETED)
            verify(occupyLocationUseCase).occupy(OccupyLocationCommand(1L, 5))
            verify(registerInventoryUseCase).register(RegisterInventoryCommand(500L, 1L, 5, 1L))
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 5))
        }

        @Test
        fun `불량 판정 상품은 DEFECTIVE Inventory로 등록되고 반품 불량 사유로 폐기 등록과 연계된다`() = runTest {
            val existingReturnRequest: ReturnRequest =
                returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.INSPECTING).build()
            val item: ReturnItem = returnItem()
                .returnItemId(1L)
                .returnRequestId(1L)
                .productId(100L)
                .expectedQuantity(5)
                .actualQuantity(5)
                .inspectionResult(ReturnInspectionResult.DEFECTIVE)
                .build()
            whenever(returnRequestRepository.findById(1L)).thenReturn(existingReturnRequest)
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))
            whenever(getProductUseCase.getById(100L)).thenReturn(productResult(100L))
            whenever(getZoneUseCase.getByWarehouseIdAndZoneCode(1L, ZoneCode.A))
                .thenReturn(Mono.just(zoneResult(10L, ZoneCode.A)))
            whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(emptyFlow())
            val registeredLot = LotResult(500L, "LOT-1", 100L, null, null, LotStatus.NORMAL)
            whenever(registerLotUseCase.register(any())).thenReturn(registeredLot)
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(Flux.just(locationResult(1L, 10L, 70, 10)))
            whenever(occupyLocationUseCase.occupy(any())).thenReturn(Mono.just(locationResult(1L, 10L, 70, 15)))
            val registeredInventory = InventoryResult(1L, 100L, 500L, 1L, 5, 0, 5, QualityStatus.NORMAL)
            whenever(registerInventoryUseCase.register(any())).thenReturn(registeredInventory)
            whenever(markInventoryDefectiveUseCase.markDefective(1L))
                .thenReturn(registeredInventory.copy(qualityStatus = QualityStatus.DEFECTIVE))
            whenever(registerDisposalUseCase.register(any()))
                .thenReturn(DisposalResult(900L, 1L, DisposalStatus.REQUESTED))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.RETURN)).thenReturn(Mono.just(workAreaResult(5)))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(workAreaResult(0)))
            whenever(returnRequestRepository.save(any())).thenAnswer { it.getArgument(0) }

            val command = CompleteReturnRequestCommand(1L, listOf(ReturnItemLotAssignmentCommand(1L, "LOT-1", null, null)))
            val result = returnRequestService.complete(command)

            assertThat(result.status).isEqualTo(ReturnRequestStatus.COMPLETED)
            verify(markInventoryDefectiveUseCase).markDefective(1L)
            verify(registerDisposalUseCase).register(
                RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(1L, 5, DisposalReason.RETURN_DEFECT)))
            )
        }

        @Test
        fun `검수 완료 상품에 Lot 정보가 없으면 예외를 던진다`() = runTest {
            val existingReturnRequest: ReturnRequest =
                returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.INSPECTING).build()
            val item: ReturnItem = returnItem()
                .returnItemId(1L).returnRequestId(1L)
                .actualQuantity(5).inspectionResult(ReturnInspectionResult.NORMAL).build()
            whenever(returnRequestRepository.findById(1L)).thenReturn(existingReturnRequest)
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))

            assertThatThrownBy {
                runBlocking { returnRequestService.complete(CompleteReturnRequestCommand(1L, emptyList())) }
            }.isInstanceOf(MissingLotAssignmentException::class.java)
        }

        @Test
        fun `Zone 잔여 capacity가 부족하면 예외를 던진다`() = runTest {
            val existingReturnRequest: ReturnRequest =
                returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.INSPECTING).build()
            val item: ReturnItem = returnItem()
                .returnItemId(1L).returnRequestId(1L).productId(100L)
                .actualQuantity(100).inspectionResult(ReturnInspectionResult.NORMAL).build()
            whenever(returnRequestRepository.findById(1L)).thenReturn(existingReturnRequest)
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))
            whenever(getProductUseCase.getById(100L)).thenReturn(productResult(100L))
            whenever(getZoneUseCase.getByWarehouseIdAndZoneCode(1L, ZoneCode.A))
                .thenReturn(Mono.just(zoneResult(10L, ZoneCode.A)))
            whenever(getLotUseCase.getAllByProduct(100L)).thenReturn(emptyFlow())
            whenever(registerLotUseCase.register(any()))
                .thenReturn(LotResult(500L, "LOT-1", 100L, null, null, LotStatus.NORMAL))
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(Flux.just(locationResult(1L, 10L, 70, 10)))
            whenever(occupyLocationUseCase.occupy(any())).thenReturn(Mono.just(locationResult(1L, 10L, 70, 70)))
            whenever(registerInventoryUseCase.register(any()))
                .thenReturn(InventoryResult(1L, 100L, 500L, 1L, 60, 0, 60, QualityStatus.NORMAL))

            val command = CompleteReturnRequestCommand(1L, listOf(ReturnItemLotAssignmentCommand(1L, "LOT-1", null, null)))
            assertThatThrownBy { runBlocking { returnRequestService.complete(command) } }
                .isInstanceOf(InsufficientZoneCapacityException::class.java)
        }

        @Test
        fun `검수되지 않은 상품이 있으면 예외를 던진다`() = runTest {
            val existingReturnRequest: ReturnRequest =
                returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.INSPECTING).build()
            val item: ReturnItem = returnItem().returnItemId(1L).returnRequestId(1L).build()
            whenever(returnRequestRepository.findById(1L)).thenReturn(existingReturnRequest)
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))

            assertThatThrownBy {
                runBlocking { returnRequestService.complete(CompleteReturnRequestCommand(1L, emptyList())) }
            }.isInstanceOf(NotAllReturnItemsInspectedException::class.java)
        }

        @Test
        fun `존재하지 않는 반품을 완료하면 예외를 던진다`() = runTest {
            whenever(returnRequestRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy {
                runBlocking { returnRequestService.complete(CompleteReturnRequestCommand(1L, emptyList())) }
            }.isInstanceOf(ReturnRequestNotFoundException::class.java)
        }
    }

    @Nested
    inner class 단건_조회 {

        @Test
        fun `존재하지 않는 반품을 조회하면 예외를 던진다`() = runTest {
            whenever(returnRequestRepository.findById(999L)).thenReturn(null)

            assertThatThrownBy { runBlocking { returnRequestService.getById(999L) } }
                .isInstanceOf(ReturnRequestNotFoundException::class.java)
        }
    }
}
