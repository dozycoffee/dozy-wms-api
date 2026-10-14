package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.global.security.AllWarehouses
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccess
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.inbound.application.port.`in`.command.InboundReceiptCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundReceiptRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inbound.domain.exception.ExpirationDateNotAllowedException
import com.dozycoffee.wms.inbound.domain.exception.ExpirationDateRequiredException
import com.dozycoffee.wms.inbound.domain.exception.ExpiredReceiptCannotBeNormalException
import com.dozycoffee.wms.inbound.domain.exception.InboundItemNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InboundOverReceivedException
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
import com.dozycoffee.wms.inbound.fixture.InboundReceiptTestBuilder.Companion.inboundReceipt
import com.dozycoffee.wms.inbound.fixture.InboundTestBuilder.Companion.inbound
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.support.warehouseAccessGuardOf
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
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class InboundItemServiceTest {

    @Mock
    private lateinit var inboundItemRepository: InboundItemRepository

    @Mock
    private lateinit var inboundReceiptRepository: InboundReceiptRepository

    @Mock
    private lateinit var inboundRepository: InboundRepository

    @Mock
    private lateinit var getProductUseCase: GetProductUseCase

    private lateinit var inboundItemService: InboundItemService

    private val today: LocalDate = LocalDate.now()

    @BeforeEach
    fun setUp() {
        inboundItemService = newService(AllWarehouses)
        runBlocking {
            lenient().`when`(inboundRepository.findById(1L))
                .thenReturn(inbound().inboundId(1L).warehouseId(1L).build())
        }
    }

    private fun newService(access: WarehouseAccess): InboundItemService =
        InboundItemService(
            inboundItemRepository,
            inboundReceiptRepository,
            inboundRepository,
            getProductUseCase,
            warehouseAccessGuardOf(access)
        )

    private fun productResult(shelfLifeDays: Int?): ProductResult =
        ProductResult(100L, "P-100", "상품100", ProductCategory.SYRUP, "EA", shelfLifeDays, ProductStatus.ACTIVE)

    private fun receiptCommand(
        lotNumber: String = "LOT-1",
        expirationDate: LocalDate? = today.plusDays(100),
        quantity: Int = 10,
        result: InspectionResult = InspectionResult.NORMAL,
        defectReason: DefectReason? = null
    ): InboundReceiptCommand = InboundReceiptCommand(lotNumber, null, expirationDate, quantity, result, defectReason)

    private fun pendingItem(expectedQuantity: Int = 10): InboundItem =
        inboundItem().inboundItemId(1L).inboundId(1L).productId(100L).expectedQuantity(expectedQuantity).build()

    private suspend fun stubItemAndProduct(item: InboundItem, shelfLifeDays: Int?) {
        whenever(inboundItemRepository.findById(1L)).thenReturn(item)
        whenever(getProductUseCase.getById(100L)).thenReturn(productResult(shelfLifeDays))
    }

    private suspend fun stubSaveAllWithIds() {
        whenever(inboundReceiptRepository.saveAll(any())).thenAnswer {
            it.getArgument<List<InboundReceipt>>(0).mapIndexed { index, receipt ->
                InboundReceipt.reconstitute(
                    index + 1L, receipt.inboundItemId, receipt.lotNumber, receipt.manufactureDate,
                    receipt.expirationDate, receipt.quantity, receipt.inspectionResult, receipt.defectReason
                )
            }
        }
    }

    @Nested
    inner class 검수 {

        @Test
        fun `수령 라인을 저장하고 검수 완료 상태와 실제 수량을 기록한다`() = runTest {
            stubItemAndProduct(pendingItem(), 365)
            whenever(inboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
            stubSaveAllWithIds()

            val result = inboundItemService.inspect(
                InspectInboundItemCommand(
                    1L,
                    listOf(
                        receiptCommand(quantity = 8),
                        receiptCommand(quantity = 2, result = InspectionResult.DEFECTIVE, defectReason = DefectReason.DAMAGED)
                    )
                )
            )

            assertThat(result.actualQuantity).isEqualTo(10)
            assertThat(result.inspectionStatus).isEqualTo(InspectionStatus.INSPECTED)
            assertThat(result.receipts).hasSize(2)
            assertThat(result.receipts.map { it.quantity }).containsExactly(8, 2)
            verify(inboundReceiptRepository).saveAll(any())
        }

        @Test
        fun `유통기한을 관리하지 않는 상품은 유통기한 없이 검수할 수 있다`() = runTest {
            stubItemAndProduct(pendingItem(), null)
            whenever(inboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
            stubSaveAllWithIds()

            val result = inboundItemService.inspect(
                InspectInboundItemCommand(1L, listOf(receiptCommand(expirationDate = null)))
            )

            assertThat(result.inspectionStatus).isEqualTo(InspectionStatus.INSPECTED)
        }

        @Test
        fun `수령 라인이 없으면 미도착으로 수량 0을 기록한다`() = runTest {
            stubItemAndProduct(pendingItem(), 365)
            whenever(inboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
            whenever(inboundReceiptRepository.saveAll(any())).thenReturn(emptyList())

            val result = inboundItemService.inspect(InspectInboundItemCommand(1L, emptyList()))

            assertThat(result.actualQuantity).isZero()
            assertThat(result.quantityDiscrepancy).isEqualTo(-10)
        }

        @Test
        fun `유통기한 관리 상품에 유통기한이 없으면 예외를 던진다`() = runTest {
            stubItemAndProduct(pendingItem(), 365)

            assertThatThrownBy {
                runBlocking { inboundItemService.inspect(InspectInboundItemCommand(1L, listOf(receiptCommand(expirationDate = null)))) }
            }.isInstanceOf(ExpirationDateRequiredException::class.java)
            verify(inboundItemRepository, never()).save(any())
        }

        @Test
        fun `유통기한을 관리하지 않는 상품에 유통기한을 지정하면 예외를 던진다`() = runTest {
            stubItemAndProduct(pendingItem(), null)

            assertThatThrownBy {
                runBlocking { inboundItemService.inspect(InspectInboundItemCommand(1L, listOf(receiptCommand()))) }
            }.isInstanceOf(ExpirationDateNotAllowedException::class.java)
            verify(inboundItemRepository, never()).save(any())
        }

        @Test
        fun `유통기한이 지난 라인을 정상으로 판정하면 예외를 던진다`() = runTest {
            stubItemAndProduct(pendingItem(), 365)

            assertThatThrownBy {
                runBlocking {
                    inboundItemService.inspect(
                        InspectInboundItemCommand(1L, listOf(receiptCommand(expirationDate = today.minusDays(1))))
                    )
                }
            }.isInstanceOf(ExpiredReceiptCannotBeNormalException::class.java)
            verify(inboundItemRepository, never()).save(any())
        }

        @Test
        fun `유통기한이 지난 라인을 불량으로 판정하면 기록된다`() = runTest {
            stubItemAndProduct(pendingItem(), 365)
            whenever(inboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }
            stubSaveAllWithIds()

            val result = inboundItemService.inspect(
                InspectInboundItemCommand(
                    1L,
                    listOf(
                        receiptCommand(
                            expirationDate = today.minusDays(1),
                            result = InspectionResult.DEFECTIVE,
                            defectReason = DefectReason.EXPIRED
                        )
                    )
                )
            )

            assertThat(result.inspectionStatus).isEqualTo(InspectionStatus.INSPECTED)
        }

        @Test
        fun `수령 수량 합이 예정 수량을 초과하면 예외를 던진다`() = runTest {
            stubItemAndProduct(pendingItem(10), 365)

            assertThatThrownBy {
                runBlocking { inboundItemService.inspect(InspectInboundItemCommand(1L, listOf(receiptCommand(quantity = 11)))) }
            }.isInstanceOf(InboundOverReceivedException::class.java)
            verify(inboundReceiptRepository, never()).saveAll(any())
            verify(inboundItemRepository, never()).save(any())
        }

        @Test
        fun `존재하지 않는 입고 상품을 검수하면 예외를 던진다`() = runTest {
            whenever(inboundItemRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy {
                runBlocking { inboundItemService.inspect(InspectInboundItemCommand(1L, emptyList())) }
            }.isInstanceOf(InboundItemNotFoundException::class.java)
        }
    }

    @Nested
    inner class 입고별_상품_목록_조회 {

        @Test
        fun `입고 ID로 상품 목록을 수령 라인과 함께 조회한다`() = runTest {
            val item: InboundItem = inboundItem().inboundItemId(1L).build()
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(item))
            whenever(inboundReceiptRepository.findAllByInboundItemIds(listOf(1L))).thenReturn(
                listOf(
                    InboundReceipt.reconstitute(7L, 1L, "LOT-1", null, null, 10, InspectionResult.NORMAL, null)
                )
            )

            val result = inboundItemService.getAllByInbound(1L).toList()

            assertThat(result).hasSize(1)
            assertThat(result[0].receipts).hasSize(1)
            assertThat(result[0].receipts[0].lotNumber).isEqualTo("LOT-1")
        }

        @Test
        fun `검수 전 상품은 수령 라인이 비어 있다`() = runTest {
            val item: InboundItem = inboundItem().inboundItemId(1L).build()
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(item))
            whenever(inboundReceiptRepository.findAllByInboundItemIds(listOf(1L))).thenReturn(emptyList())

            val result = inboundItemService.getAllByInbound(1L).toList()

            assertThat(result[0].receipts).isEmpty()
        }
    }

    @Nested
    inner class 창고_접근 {

        private fun deniedService(): InboundItemService = newService(OnlyWarehouses(setOf(2L)))

        @Test
        fun `접근할 수 없는 창고의 목록은 조회할 수 없다`() = runTest {
            assertThatThrownBy { runBlocking { deniedService().getAllByInbound(1L).toList() } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(inboundItemRepository, never()).findAllByInboundId(any())
        }

        @Test
        fun `상위 문서가 없으면 목록 조회는 예외를 던진다`() = runTest {
            whenever(inboundRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { inboundItemService.getAllByInbound(1L).toList() } }
                .isInstanceOf(InboundNotFoundException::class.java)
        }

        @Test
        fun `접근할 수 없는 창고의 항목은 처리할 수 없다`() = runTest {
            val item = inboundItem().inboundItemId(1L).build()
            whenever(inboundItemRepository.findById(1L)).thenReturn(item)

            assertThatThrownBy { runBlocking { deniedService().inspect(InspectInboundItemCommand(1L, emptyList())) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(inboundItemRepository, never()).save(any())
        }
    }
}
