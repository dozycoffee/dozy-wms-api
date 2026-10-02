package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.InboundItemNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
import com.dozycoffee.wms.inbound.fixture.InboundTestBuilder.Companion.inbound
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
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.Mockito.lenient
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class InboundItemServiceTest {

    @Mock
    private lateinit var inboundItemRepository: InboundItemRepository

    @Mock
    private lateinit var inboundRepository: InboundRepository

    @Spy
    private var warehouseAccessGuard: WarehouseAccessGuard = warehouseAccessGuardOf()

    @InjectMocks
    private lateinit var inboundItemService: InboundItemService

    @BeforeEach
    fun setUp() {
        runBlocking {
            lenient().`when`(inboundRepository.findById(1L))
                .thenReturn(inbound().inboundId(1L).warehouseId(1L).build())
        }
    }

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

    @Nested
    inner class 창고_접근 {

        private fun deniedService(): InboundItemService =
            InboundItemService(inboundItemRepository, inboundRepository, warehouseAccessGuardOf(OnlyWarehouses(setOf(2L))))

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

            assertThatThrownBy { runBlocking { deniedService().inspect(InspectInboundItemCommand(1L, 10, InspectionResult.NORMAL)) } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(inboundItemRepository, never()).save(any())
        }
    }
}
