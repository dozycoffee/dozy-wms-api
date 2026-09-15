package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalItemTestBuilder.Companion.disposalItem
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
class DisposalItemServiceTest {

    @Mock
    private lateinit var disposalItemRepository: DisposalItemRepository

    @InjectMocks
    private lateinit var disposalItemService: DisposalItemService

    @Nested
    inner class 폐기별_상품_목록_조회 {

        @Test
        fun `폐기 ID로 상품 목록을 조회한다`() = runTest {
            val item: DisposalItem = disposalItem().disposalItemId(1L).build()
            whenever(disposalItemRepository.findAllByDisposalId(1L)).thenReturn(flowOf(item))

            val result = disposalItemService.getAllByDisposal(1L).toList()

            assertThat(result).hasSize(1)
            assertThat(result.first().disposalItemId).isEqualTo(1L)
        }
    }
}
