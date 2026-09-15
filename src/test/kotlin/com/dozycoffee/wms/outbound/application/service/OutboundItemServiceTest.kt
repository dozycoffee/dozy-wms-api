package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.outbound.fixture.OutboundItemTestBuilder.Companion.outboundItem
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class OutboundItemServiceTest {

    @Mock
    private lateinit var outboundItemRepository: OutboundItemRepository

    @InjectMocks
    private lateinit var outboundItemService: OutboundItemService

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
}
