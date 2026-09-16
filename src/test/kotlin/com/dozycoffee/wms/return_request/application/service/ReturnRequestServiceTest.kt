package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnItemCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.out.ReturnItemRepository
import com.dozycoffee.wms.return_request.application.port.out.ReturnRequestRepository
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.exception.NotAllReturnItemsInspectedException
import com.dozycoffee.wms.return_request.domain.exception.ReturnRequestNotFoundException
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import com.dozycoffee.wms.return_request.fixture.ReturnItemTestBuilder.Companion.returnItem
import com.dozycoffee.wms.return_request.fixture.ReturnRequestTestBuilder.Companion.returnRequest
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @InjectMocks
    private lateinit var returnRequestService: ReturnRequestService

    private fun workAreaResult(usedCapacity: Int): WorkAreaResult {
        return WorkAreaResult(
            1L, 1L, AreaCode.RETURN, AreaCode.RETURN.areaName, AreaCode.RETURN.capacity.value, usedCapacity, AvailabilityStatus.AVAILABLE
        )
    }

    private fun productResult(productId: Long): ProductResult {
        return ProductResult(productId, "PRD-$productId", "상품$productId", ProductCategory.BEAN, "kg", 365, ProductStatus.ACTIVE)
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
        fun `모든 상품의 검수가 끝나면 반품 처리장 점유를 해제하고 COMPLETED로 전환한다`() = runTest {
            val existingReturnRequest: ReturnRequest =
                returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.INSPECTING).build()
            val item: ReturnItem = returnItem()
                .returnItemId(1L)
                .returnRequestId(1L)
                .expectedQuantity(5)
                .actualQuantity(5)
                .inspectionResult(ReturnInspectionResult.NORMAL)
                .build()
            whenever(returnRequestRepository.findById(1L)).thenReturn(existingReturnRequest)
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.RETURN)).thenReturn(Mono.just(workAreaResult(5)))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(workAreaResult(0)))
            whenever(returnRequestRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = returnRequestService.complete(1L)

            assertThat(result.status).isEqualTo(ReturnRequestStatus.COMPLETED)
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 5))
        }

        @Test
        fun `검수되지 않은 상품이 있으면 예외를 던진다`() = runTest {
            val existingReturnRequest: ReturnRequest =
                returnRequest().returnRequestId(1L).warehouseId(1L).status(ReturnRequestStatus.INSPECTING).build()
            val item: ReturnItem = returnItem().returnItemId(1L).returnRequestId(1L).build()
            whenever(returnRequestRepository.findById(1L)).thenReturn(existingReturnRequest)
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))

            assertThatThrownBy { runBlocking { returnRequestService.complete(1L) } }
                .isInstanceOf(NotAllReturnItemsInspectedException::class.java)
        }

        @Test
        fun `존재하지 않는 반품을 완료하면 예외를 던진다`() = runTest {
            whenever(returnRequestRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { returnRequestService.complete(1L) } }
                .isInstanceOf(ReturnRequestNotFoundException::class.java)
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
