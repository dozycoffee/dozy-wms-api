package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.return_request.application.port.`in`.command.InspectReturnItemCommand
import com.dozycoffee.wms.return_request.application.port.out.ReturnItemRepository
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.exception.ReturnItemNotFoundException
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import com.dozycoffee.wms.return_request.fixture.ReturnItemTestBuilder.Companion.returnItem
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
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ReturnItemServiceTest {

    @Mock
    private lateinit var returnItemRepository: ReturnItemRepository

    @InjectMocks
    private lateinit var returnItemService: ReturnItemService

    @Nested
    inner class 검수 {

        @Test
        fun `정상 판정 시 실제 수량과 결과가 저장된다`() = runTest {
            val item: ReturnItem = returnItem().returnItemId(1L).expectedQuantity(5).build()
            whenever(returnItemRepository.findById(1L)).thenReturn(item)
            whenever(returnItemRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = returnItemService.inspect(InspectReturnItemCommand(1L, 5, ReturnInspectionResult.NORMAL))

            assertThat(result.actualQuantity).isEqualTo(5)
            assertThat(result.inspectionResult).isEqualTo(ReturnInspectionResult.NORMAL)
        }

        @Test
        fun `존재하지 않는 반품 상품을 검수하면 예외를 던진다`() = runTest {
            whenever(returnItemRepository.findById(999L)).thenReturn(null)

            assertThatThrownBy {
                runBlocking { returnItemService.inspect(InspectReturnItemCommand(999L, 5, ReturnInspectionResult.NORMAL)) }
            }
                .isInstanceOf(ReturnItemNotFoundException::class.java)
        }
    }

    @Nested
    inner class 반품별_상품_목록_조회 {

        @Test
        fun `반품 ID로 상품 목록을 조회한다`() = runTest {
            val item: ReturnItem = returnItem().returnItemId(1L).build()
            whenever(returnItemRepository.findAllByReturnRequestId(1L)).thenReturn(flowOf(item))

            val result = returnItemService.getAllByReturnRequest(1L).toList()

            assertThat(result).hasSize(1)
            assertThat(result.first().returnItemId).isEqualTo(1L)
        }
    }
}
