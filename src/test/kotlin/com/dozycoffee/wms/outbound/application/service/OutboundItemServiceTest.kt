package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
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
class OutboundItemServiceTest {

    @Mock
    private lateinit var outboundItemRepository: OutboundItemRepository

    @Mock
    private lateinit var outboundRepository: OutboundRepository

    @Spy
    private var warehouseAccessGuard: WarehouseAccessGuard = warehouseAccessGuardOf()

    @InjectMocks
    private lateinit var outboundItemService: OutboundItemService

    @BeforeEach
    fun setUp() {
        runBlocking {
            lenient().`when`(outboundRepository.findById(1L))
                .thenReturn(outbound().outboundId(1L).warehouseId(1L).build())
        }
    }

    @Nested
    inner class 출고별_상품_목록_조회 {

        @Test
        fun `출고 ID로 상품 목록을 조회한다`() = runTest {
            val item: OutboundItem = outboundItem().outboundItemId(1L).build()
            whenever(outboundItemRepository.findAllByOutboundId(1L)).thenReturn(flowOf(item))

            val result = outboundItemService.getAllByOutbound(1L).toList()

            assertThat(result).hasSize(1)
            assertThat(result.first().outboundItemId).isEqualTo(1L)
        }
    }

    @Nested
    inner class 창고_접근 {

        private fun deniedService(): OutboundItemService =
            OutboundItemService(outboundItemRepository, outboundRepository, warehouseAccessGuardOf(OnlyWarehouses(setOf(2L))))

        @Test
        fun `접근할 수 없는 창고의 목록은 조회할 수 없다`() = runTest {
            assertThatThrownBy { runBlocking { deniedService().getAllByOutbound(1L).toList() } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(outboundItemRepository, never()).findAllByOutboundId(any())
        }

        @Test
        fun `상위 문서가 없으면 목록 조회는 예외를 던진다`() = runTest {
            whenever(outboundRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { outboundItemService.getAllByOutbound(1L).toList() } }
                .isInstanceOf(OutboundNotFoundException::class.java)
        }
    }
}
