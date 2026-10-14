package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.location_stock.application.port.`in`.GetLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.IncreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
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
import com.dozycoffee.wms.return_request.domain.exception.ReturnLotNotRegisteredException
import com.dozycoffee.wms.return_request.domain.exception.NotAllReturnItemsInspectedException
import com.dozycoffee.wms.return_request.domain.exception.ReturnRequestNotFoundException
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import com.dozycoffee.wms.return_request.fixture.ReturnItemTestBuilder.Companion.returnItem
import com.dozycoffee.wms.return_request.fixture.ReturnRequestTestBuilder.Companion.returnRequest
import com.dozycoffee.wms.support.SwitchableWarehouseAccess
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
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
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @Mock
    private lateinit var getLocationStockUseCase: GetLocationStockUseCase

    @Mock
    private lateinit var increaseLocationStockUseCase: IncreaseLocationStockUseCase

    @Mock
    private lateinit var returnInventoryCallRecorder: ReturnInventoryCallRecorder

    @Mock
    private lateinit var registerDisposalUseCase: RegisterDisposalUseCase

    private val warehouseAccess = SwitchableWarehouseAccess()

    @Spy
    private var warehouseAccessGuard: WarehouseAccessGuard = WarehouseAccessGuard(warehouseAccess)

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
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.RETURN)).thenReturn(workAreaResult(0))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(workAreaResult(5))
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

        private fun inspectedItem(
            result: ReturnInspectionResult,
            actualQuantity: Int
        ): ReturnItem = returnItem()
            .returnItemId(1L).returnRequestId(1L).productId(100L)
            .expectedQuantity(5).actualQuantity(actualQuantity).inspectionResult(result).build()

        private fun inspectingRequest(): ReturnRequest =
            returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.INSPECTING).build()

        private suspend fun stubRequestAndItem(item: ReturnItem) {
            whenever(returnRequestRepository.findById(1L)).thenReturn(inspectingRequest())
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))
        }

        private suspend fun stubPlacement(maxCapacity: Int = 70, usedCapacity: Int = 10) {
            whenever(getProductUseCase.getById(100L)).thenReturn(productResult(100L))
            whenever(getZoneUseCase.getByWarehouseIdAndZoneCode(1L, ZoneCode.A)).thenReturn(zoneResult(10L, ZoneCode.A))
            whenever(getLocationUseCase.getByZoneId(10L)).thenReturn(flowOf(locationResult(1L, 10L, maxCapacity, usedCapacity)))
        }

        private suspend fun stubIncrease() {
            whenever(increaseLocationStockUseCase.increase(any())).thenAnswer {
                val command = it.getArgument<IncreaseLocationStockCommand>(0)
                LocationStockResult(
                    command.locationId * 100 + command.quantity, command.locationId, command.productId,
                    command.lotNumber, command.qualityStatus, command.quantity
                )
            }
        }

        private suspend fun stubRelease() {
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.RETURN)).thenReturn(workAreaResult(5))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(workAreaResult(0))
            whenever(returnRequestRepository.save(any())).thenAnswer { it.getArgument(0) }
        }

        private fun assignment(lotNumber: String = "LOT-1") =
            CompleteReturnRequestCommand(1L, listOf(ReturnItemLotAssignmentCommand(1L, lotNumber)))

        @Test
        fun `정상 판정 상품은 등록된 Lot인지 확인하고 Location에 분산 배치해 정상 품질 장부에 반영한다`() = runTest {
            stubRequestAndItem(inspectedItem(ReturnInspectionResult.NORMAL, 5))
            whenever(getLocationStockUseCase.existsLot(100L, "LOT-1")).thenReturn(true)
            stubPlacement()
            stubIncrease()
            stubRelease()

            val result = returnRequestService.complete(assignment())

            assertThat(result.status).isEqualTo(ReturnRequestStatus.COMPLETED)
            verify(increaseLocationStockUseCase).increase(IncreaseLocationStockCommand(1L, 100L, "LOT-1", StockQualityStatus.NORMAL, 5))
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 5))
            verify(registerDisposalUseCase, never()).register(any())
        }

        @Test
        fun `불량 판정 상품은 불량 품질로 반영하고 반품 불량 사유로 폐기 등록과 연계된다`() = runTest {
            stubRequestAndItem(inspectedItem(ReturnInspectionResult.DEFECTIVE, 5))
            whenever(getLocationStockUseCase.existsLot(100L, "LOT-1")).thenReturn(true)
            stubPlacement()
            stubIncrease()
            stubRelease()
            whenever(registerDisposalUseCase.register(any())).thenReturn(DisposalResult(900L, 1L, DisposalStatus.REQUESTED))

            returnRequestService.complete(assignment())

            verify(increaseLocationStockUseCase).increase(IncreaseLocationStockCommand(1L, 100L, "LOT-1", StockQualityStatus.DEFECTIVE, 5))
            verify(registerDisposalUseCase).register(
                RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(105L, 5, DisposalReason.RETURN_DEFECT)))
            )
        }

        @Test
        fun `등록되지 않은 Lot이면 예외를 던지고 장부 반영도 호출 기록도 하지 않는다`() = runTest {
            stubRequestAndItem(inspectedItem(ReturnInspectionResult.NORMAL, 5))
            whenever(getLocationStockUseCase.existsLot(100L, "LOT-UNKNOWN")).thenReturn(false)

            assertThatThrownBy { runBlocking { returnRequestService.complete(assignment("LOT-UNKNOWN")) } }
                .isInstanceOf(ReturnLotNotRegisteredException::class.java)
            verify(increaseLocationStockUseCase, never()).increase(any())
            verify(returnInventoryCallRecorder, never()).record(any(), any(), any())
        }

        @Test
        fun `반품 상품마다 inventory 반품 확정 호출을 기록한다`() = runTest {
            val item = inspectedItem(ReturnInspectionResult.NORMAL, 5)
            stubRequestAndItem(item)
            whenever(getLocationStockUseCase.existsLot(100L, "LOT-1")).thenReturn(true)
            stubPlacement()
            stubIncrease()
            stubRelease()

            returnRequestService.complete(assignment())

            verify(returnInventoryCallRecorder).record(1L, item, "LOT-1")
        }

        @Test
        fun `실제 수량이 0이면 호출을 기록하지 않는다`() = runTest {
            stubRequestAndItem(inspectedItem(ReturnInspectionResult.NORMAL, 0))
            whenever(getLocationStockUseCase.existsLot(100L, "LOT-1")).thenReturn(true)
            stubPlacement()
            stubRelease()

            returnRequestService.complete(assignment())

            verify(returnInventoryCallRecorder, never()).record(any(), any(), any())
            verify(increaseLocationStockUseCase, never()).increase(any())
        }

        @Test
        fun `검수 완료 상품에 Lot 정보가 없으면 예외를 던진다`() = runTest {
            stubRequestAndItem(inspectedItem(ReturnInspectionResult.NORMAL, 5))

            assertThatThrownBy {
                runBlocking { returnRequestService.complete(CompleteReturnRequestCommand(1L, emptyList())) }
            }.isInstanceOf(MissingLotAssignmentException::class.java)
        }

        @Test
        fun `Zone 잔여 capacity가 부족하면 예외를 던진다`() = runTest {
            stubRequestAndItem(inspectedItem(ReturnInspectionResult.NORMAL, 100))
            whenever(getLocationStockUseCase.existsLot(100L, "LOT-1")).thenReturn(true)
            stubPlacement()
            stubIncrease()

            assertThatThrownBy { runBlocking { returnRequestService.complete(assignment()) } }
                .isInstanceOf(InsufficientZoneCapacityException::class.java)
        }

        @Test
        fun `검수되지 않은 상품이 있으면 예외를 던진다`() = runTest {
            stubRequestAndItem(returnItem().returnItemId(1L).returnRequestId(1L).build())

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

    @Nested
    inner class 창고_접근 {

        @Test
        fun `접근할 수 없는 창고에는 등록할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))

            assertThatThrownBy { runBlocking { returnRequestService.register(RegisterReturnRequestCommand(1L, listOf(RegisterReturnItemCommand(10L, 5)))) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(returnRequestRepository, never()).save(any())
        }

        @Test
        fun `접근할 수 없는 창고의 문서는 단건 조회할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))
            whenever(returnRequestRepository.findById(1L)).thenReturn(returnRequest().returnRequestId(1L).warehouseId(1L).build())

            assertThatThrownBy { runBlocking { returnRequestService.getById(1L) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
        }

        @Test
        fun `목록은 접근 가능한 창고로 좁혀 조회한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(1L, 3L))
            whenever(returnRequestRepository.findAll(null, listOf(1L, 3L))).thenReturn(flowOf())

            returnRequestService.getAll(null).toList()

            verify(returnRequestRepository).findAll(null, listOf(1L, 3L))
        }

        @Test
        fun `접근 가능한 창고가 없으면 저장소를 조회하지 않고 빈 목록을 반환한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(emptySet())

            val result = returnRequestService.getAll(null).toList()

            assertThat(result).isEmpty()
            verify(returnRequestRepository, never()).findAll(any(), any())
        }

        @Test
        fun `전체 창고 접근이면 창고 조건 없이 조회한다`() = runTest {
            whenever(returnRequestRepository.findAll(null, null)).thenReturn(flowOf())

            returnRequestService.getAll(null).toList()

            verify(returnRequestRepository).findAll(null, null)
        }
    }
}
