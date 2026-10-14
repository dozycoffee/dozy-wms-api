package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.inbound.application.port.`in`.command.CompleteInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundItemCommand
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundReceiptRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inbound.fixture.InboundReceiptTestBuilder.Companion.inboundReceipt
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InsufficientZoneCapacityException
import com.dozycoffee.wms.inbound.domain.exception.NotAllItemsInspectedException
import com.dozycoffee.wms.inbound.domain.model.Inbound
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
import com.dozycoffee.wms.inbound.fixture.InboundTestBuilder.Companion.inbound
import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import tools.jackson.databind.json.JsonMapper
import com.dozycoffee.wms.location_stock.application.port.`in`.IncreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
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
import java.time.LocalDate
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
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @Mock
    private lateinit var inboundReceiptRepository: InboundReceiptRepository

    @Mock
    private lateinit var increaseLocationStockUseCase: IncreaseLocationStockUseCase

    @Mock
    private lateinit var registerDisposalUseCase: RegisterDisposalUseCase

    @Mock
    private lateinit var recordInventoryCallUseCase: RecordInventoryCallUseCase

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    private var receiptSequence: Long = 0L

    private fun nextReceiptId(): Long = ++receiptSequence

    private val warehouseAccess = SwitchableWarehouseAccess()

    @Spy
    private var warehouseAccessGuard: WarehouseAccessGuard = WarehouseAccessGuard(warehouseAccess)

    private lateinit var inboundService: InboundService

    @BeforeEach
    fun setUp() {
        inboundService = InboundService(
            warehouseAccessGuard,
            inboundRepository,
            inboundItemRepository,
            inboundReceiptRepository,
            getProductUseCase,
            getZoneUseCase,
            getLocationUseCase,
            getWorkAreaUseCase,
            occupyWorkAreaUseCase,
            releaseWorkAreaUseCase,
            increaseLocationStockUseCase,
            registerDisposalUseCase,
            InboundInventoryCallRecorder(recordInventoryCallUseCase, jsonMapper)
        )
    }

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
                .thenReturn(zoneResult(10L, ZoneCode.A))
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(flowOf(locationResult(1L, 10L, 70, 10)))
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
                .thenReturn(zoneResult(10L, ZoneCode.A))
            whenever(getLocationUseCase.getByZoneId(10L))
                .thenReturn(flowOf(locationResult(1L, 10L, 70, 10)))

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
                .thenReturn(workAreaResult(0))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(workAreaResult(30))
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

        private suspend fun stubLocations(vararg locations: com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult) {
            whenever(getLocationUseCase.getByZoneId(10L)).thenReturn(flowOf(*locations))
            stubCompletion()
        }

        private suspend fun stubCompletion() {
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND)).thenReturn(workAreaResult(30))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(workAreaResult(0))
            whenever(inboundRepository.save(any())).thenAnswer { it.getArgument(0) }
        }

        /** 증가 요청마다 장부 행 ID를 수량으로 삼아 돌려준다 — 어떤 요청이 어떤 행을 만들었는지 구분하기 쉽다 */
        private suspend fun stubIncrease() {
            whenever(increaseLocationStockUseCase.increase(any())).thenAnswer {
                val command = it.getArgument<IncreaseLocationStockCommand>(0)
                LocationStockResult(
                    command.locationId * 100 + command.quantity,
                    command.locationId,
                    command.productId,
                    command.lotNumber,
                    command.qualityStatus,
                    command.quantity
                )
            }
        }

        private fun inspectedItem(expectedQuantity: Int = 30, actualQuantity: Int = 30): InboundItem =
            inboundItem()
                .inboundItemId(1L).inboundId(1L).productId(100L).zoneId(10L)
                .expectedQuantity(expectedQuantity).actualQuantity(actualQuantity)
                .inspectionStatus(InspectionStatus.INSPECTED)
                .build()

        private fun processingInbound(): Inbound =
            inbound().inboundId(1L).warehouseId(1L).status(InboundStatus.PROCESSING).build()

        private fun stubInbound(item: InboundItem, vararg receipts: com.dozycoffee.wms.inbound.domain.model.InboundReceipt) = runBlocking {
            whenever(inboundRepository.findById(1L)).thenReturn(processingInbound())
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(item))
            whenever(inboundReceiptRepository.findAllByInboundItemIds(listOf(1L))).thenReturn(receipts.toList())
        }

        @Test
        fun `정상 수령 라인은 Location에 분산 배치해 정상 품질 장부에 반영한다`() = runTest {
            stubInbound(inspectedItem(), inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).lotNumber("LOT-1").quantity(30).build())
            stubLocations(locationResult(1L, 10L, 70, 10))
            stubIncrease()

            val result = inboundService.complete(CompleteInboundCommand(1L))

            assertThat(result.status).isEqualTo(InboundStatus.COMPLETED)
            verify(increaseLocationStockUseCase)
                .increase(IncreaseLocationStockCommand(1L, 100L, "LOT-1", StockQualityStatus.NORMAL, 30))
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 30))
            verify(registerDisposalUseCase, never()).register(any())
        }

        @Test
        fun `한 Location에 다 들어가지 않으면 잔여 capacity가 큰 Location부터 나눠 반영한다`() = runTest {
            stubInbound(inspectedItem(), inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).lotNumber("LOT-1").quantity(30).build())
            stubLocations(locationResult(1L, 10L, 70, 60), locationResult(2L, 10L, 70, 50))
            stubIncrease()

            inboundService.complete(CompleteInboundCommand(1L))

            val captor = argumentCaptor<IncreaseLocationStockCommand>()
            verify(increaseLocationStockUseCase, times(2)).increase(captor.capture())
            assertThat(captor.allValues.map { it.locationId to it.quantity }).containsExactly(2L to 20, 1L to 10)
        }

        @Test
        fun `같은 상품의 로트가 여러 개이면 라인마다 장부에 반영한다`() = runTest {
            stubInbound(
                inspectedItem(30, 30),
                inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).lotNumber("LOT-A").quantity(20).build(),
                inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).lotNumber("LOT-B").quantity(10).build()
            )
            stubLocations(locationResult(1L, 10L, 70, 10))
            stubIncrease()

            inboundService.complete(CompleteInboundCommand(1L))

            verify(increaseLocationStockUseCase).increase(IncreaseLocationStockCommand(1L, 100L, "LOT-A", StockQualityStatus.NORMAL, 20))
            verify(increaseLocationStockUseCase).increase(IncreaseLocationStockCommand(1L, 100L, "LOT-B", StockQualityStatus.NORMAL, 10))
        }

        @Test
        fun `불량 수령 라인은 불량 품질로 장부에 반영하고 불량 사유에 맞춰 폐기 등록과 연계한다`() = runTest {
            stubInbound(inspectedItem(), inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).quantity(30).defective(DefectReason.DAMAGED).build())
            stubLocations(locationResult(1L, 10L, 70, 10))
            stubIncrease()
            whenever(registerDisposalUseCase.register(any())).thenReturn(DisposalResult(900L, 1L, DisposalStatus.REQUESTED))

            val result = inboundService.complete(CompleteInboundCommand(1L))

            assertThat(result.status).isEqualTo(InboundStatus.COMPLETED)
            verify(increaseLocationStockUseCase)
                .increase(IncreaseLocationStockCommand(1L, 100L, "LOT-001", StockQualityStatus.DEFECTIVE, 30))
            verify(registerDisposalUseCase).register(
                RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(130L, 30, DisposalReason.INSPECTION_DEFECT)))
            )
        }

        @Test
        fun `유통기한 경과 사유의 불량 라인은 폐기 사유 EXPIRED로 연계한다`() = runTest {
            stubInbound(
                inspectedItem(),
                inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).quantity(30)
                    .expirationDate(LocalDate.of(2026, 10, 1)).defective(DefectReason.EXPIRED).build()
            )
            stubLocations(locationResult(1L, 10L, 70, 10))
            stubIncrease()
            whenever(registerDisposalUseCase.register(any())).thenReturn(DisposalResult(900L, 1L, DisposalStatus.REQUESTED))

            inboundService.complete(CompleteInboundCommand(1L))

            verify(registerDisposalUseCase).register(
                RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(130L, 30, DisposalReason.EXPIRED)))
            )
        }

        @Test
        fun `정상 라인과 불량 라인이 섞여 있으면 불량 수량만 폐기 연계한다`() = runTest {
            stubInbound(
                inspectedItem(),
                inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).lotNumber("LOT-N").quantity(25).normal().build(),
                inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).lotNumber("LOT-D").quantity(5).defective(DefectReason.QUALITY).build()
            )
            stubLocations(locationResult(1L, 10L, 70, 10))
            stubIncrease()
            whenever(registerDisposalUseCase.register(any())).thenReturn(DisposalResult(900L, 1L, DisposalStatus.REQUESTED))

            inboundService.complete(CompleteInboundCommand(1L))

            verify(registerDisposalUseCase).register(
                RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(105L, 5, DisposalReason.INSPECTION_DEFECT)))
            )
        }

        @Test
        fun `검수되지 않은 상품이 있으면 예외를 던진다`() = runTest {
            val pendingItem: InboundItem = inboundItem().inboundItemId(1L).inboundId(1L).build()
            whenever(inboundRepository.findById(1L)).thenReturn(processingInbound())
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(pendingItem))

            assertThatThrownBy { runBlocking { inboundService.complete(CompleteInboundCommand(1L)) } }
                .isInstanceOf(NotAllItemsInspectedException::class.java)
        }

        @Test
        fun `Zone 잔여 capacity가 부족하면 예외를 던진다`() = runTest {
            stubInbound(inspectedItem(), inboundReceipt().inboundReceiptId(nextReceiptId()).inboundItemId(1L).quantity(30).build())
            whenever(getLocationUseCase.getByZoneId(10L)).thenReturn(flowOf(locationResult(1L, 10L, 70, 60)))
            stubIncrease()

            assertThatThrownBy { runBlocking { inboundService.complete(CompleteInboundCommand(1L)) } }
                .isInstanceOf(InsufficientZoneCapacityException::class.java)
        }

        @Test
        fun `미도착 상품은 장부에 반영하지 않고 예정 수량만큼 입고 처리장을 해제한다`() = runTest {
            stubInbound(inspectedItem(30, 0))
            stubCompletion()

            inboundService.complete(CompleteInboundCommand(1L))

            verify(increaseLocationStockUseCase, never()).increase(any())
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 30))
        }

        @Test
        fun `수령 라인마다 inventory 입고 확정 호출을 수령 라인 ID 기반 멱등 키로 기록한다`() = runTest {
            stubInbound(
                inspectedItem(30, 30),
                inboundReceipt().inboundReceiptId(71L).inboundItemId(1L).lotNumber("LOT-A").quantity(20)
                    .manufactureDate(LocalDate.of(2026, 9, 1)).expirationDate(LocalDate.of(2027, 1, 1)).build(),
                inboundReceipt().inboundReceiptId(72L).inboundItemId(1L).lotNumber("LOT-B").quantity(10)
                    .defective(DefectReason.DAMAGED).build()
            )
            stubLocations(locationResult(1L, 10L, 70, 10))
            stubIncrease()
            whenever(registerDisposalUseCase.register(any())).thenReturn(DisposalResult(900L, 1L, DisposalStatus.REQUESTED))

            inboundService.complete(CompleteInboundCommand(1L))

            val captor = argumentCaptor<RecordInventoryCallCommand>()
            verify(recordInventoryCallUseCase, times(2)).record(captor.capture())
            val (normalCall, defectiveCall) = captor.allValues
            assertThat(normalCall.callType).isEqualTo(InventoryCallType.INBOUND_RECEIPT)
            assertThat(normalCall.warehouseId).isEqualTo(1L)
            assertThat(normalCall.idempotencyKey).isEqualTo("wms-inbound-receipt-71")
            assertThat(normalCall.requestPath).isEqualTo("/api/v1/inbound-receipts")
            val normalBody = jsonMapper.readTree(normalCall.requestBody)
            assertThat(normalBody.get("productId").asLong()).isEqualTo(100L)
            assertThat(normalBody.get("quantity").asInt()).isEqualTo(20)
            assertThat(normalBody.get("qualityStatus").asText()).isEqualTo("NORMAL")
            assertThat(normalBody.get("lotNumber").asText()).isEqualTo("LOT-A")
            assertThat(normalBody.get("manufactureDate").asText()).isEqualTo("2026-09-01")
            assertThat(normalBody.get("expirationDate").asText()).isEqualTo("2027-01-01")
            assertThat(normalBody.get("inboundItemId").asLong()).isEqualTo(1L)
            assertThat(defectiveCall.idempotencyKey).isEqualTo("wms-inbound-receipt-72")
            assertThat(jsonMapper.readTree(defectiveCall.requestBody).get("qualityStatus").asText()).isEqualTo("DEFECTIVE")
        }

        @Test
        fun `여러 Location에 나눠 적재해도 수령 라인당 호출은 한 건이다`() = runTest {
            stubInbound(inspectedItem(), inboundReceipt().inboundReceiptId(71L).inboundItemId(1L).quantity(30).build())
            stubLocations(locationResult(1L, 10L, 50, 35), locationResult(2L, 10L, 50, 35))
            stubIncrease()

            inboundService.complete(CompleteInboundCommand(1L))

            verify(increaseLocationStockUseCase, times(2)).increase(any())
            verify(recordInventoryCallUseCase, times(1)).record(any())
        }

        @Test
        fun `미도착 상품은 호출을 기록하지 않는다`() = runTest {
            stubInbound(inspectedItem(30, 0))
            stubCompletion()

            inboundService.complete(CompleteInboundCommand(1L))

            verify(recordInventoryCallUseCase, never()).record(any())
        }
    }

    @Nested
    inner class 창고_접근 {

        @Test
        fun `접근할 수 없는 창고에는 등록할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))

            assertThatThrownBy { runBlocking { inboundService.register(RegisterInboundCommand(1L, LocalDate.of(2026, 1, 1), listOf(RegisterInboundItemCommand(100L, 30)))) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(inboundRepository, never()).save(any())
        }

        @Test
        fun `접근할 수 없는 창고의 문서는 단건 조회할 수 없다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(2L))
            whenever(inboundRepository.findById(1L)).thenReturn(inbound().inboundId(1L).warehouseId(1L).build())

            assertThatThrownBy { runBlocking { inboundService.getById(1L) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
        }

        @Test
        fun `목록은 접근 가능한 창고로 좁혀 조회한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(setOf(1L, 3L))
            whenever(inboundRepository.findAll(null, listOf(1L, 3L))).thenReturn(flowOf())

            inboundService.getAll(null).toList()

            verify(inboundRepository).findAll(null, listOf(1L, 3L))
        }

        @Test
        fun `접근 가능한 창고가 없으면 저장소를 조회하지 않고 빈 목록을 반환한다`() = runTest {
            warehouseAccess.access = OnlyWarehouses(emptySet())

            val result = inboundService.getAll(null).toList()

            assertThat(result).isEmpty()
            verify(inboundRepository, never()).findAll(any(), any())
        }

        @Test
        fun `전체 창고 접근이면 창고 조건 없이 조회한다`() = runTest {
            whenever(inboundRepository.findAll(null, null)).thenReturn(flowOf())

            inboundService.getAll(null).toList()

            verify(inboundRepository).findAll(null, null)
        }
    }
}
