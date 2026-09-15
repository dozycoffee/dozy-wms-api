package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.InboundItemNotFoundException
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
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
class InboundItemServiceTest {

    @Mock
    private lateinit var inboundItemRepository: InboundItemRepository

    @InjectMocks
    private lateinit var inboundItemService: InboundItemService

    @Nested
    inner class 검수 {

        @Test
        fun `검수 결과를 기록한다`() = runTest {
            val item: InboundItem = inboundItem().inboundItemId(1L).build()
            whenever(inboundItemRepository.findById(1L)).thenReturn(item)
            whenever(inboundItemRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = inboundItemService.inspect(InspectInboundItemCommand(1L, 10, InspectionResult.NORMAL))

            assertThat(result.actualQuantity).isEqualTo(10)
            assertThat(result.inspectionResult).isEqualTo(InspectionResult.NORMAL)
        }

        @Test
        fun `존재하지 않는 입고 상품을 검수하면 예외를 던진다`() = runTest {
            whenever(inboundItemRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy {
                runBlocking { inboundItemService.inspect(InspectInboundItemCommand(1L, 10, InspectionResult.NORMAL)) }
            }.isInstanceOf(InboundItemNotFoundException::class.java)
        }
    }

    @Nested
    inner class 입고별_상품_목록_조회 {

        @Test
        fun `입고 ID로 상품 목록을 조회한다`() = runTest {
            val item: InboundItem = inboundItem().inboundItemId(1L).build()
            whenever(inboundItemRepository.findAllByInboundId(1L)).thenReturn(flowOf(item))

            val result = inboundItemService.getAllByInbound(1L).toList()

            assertThat(result).hasSize(1)
        }
    }
}
