package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.application.port.out.DisposalRepository
import com.dozycoffee.wms.disposal.domain.exception.DisposalNotFoundException
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalItemTestBuilder.Companion.disposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalTestBuilder.Companion.disposal
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.WarehouseAccessDeniedException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
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
class DisposalItemServiceTest {

    @Mock
    private lateinit var disposalItemRepository: DisposalItemRepository

    @Mock
    private lateinit var disposalRepository: DisposalRepository

    @Spy
    private var warehouseAccessGuard: WarehouseAccessGuard = warehouseAccessGuardOf()

    @InjectMocks
    private lateinit var disposalItemService: DisposalItemService

    @BeforeEach
    fun setUp() {
        runBlocking {
            lenient().`when`(disposalRepository.findById(1L))
                .thenReturn(disposal().disposalId(1L).warehouseId(1L).build())
        }
    }

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

    @Nested
    inner class 창고_접근 {

        private fun deniedService(): DisposalItemService =
            DisposalItemService(disposalItemRepository, disposalRepository, warehouseAccessGuardOf(OnlyWarehouses(setOf(2L))))

        @Test
        fun `접근할 수 없는 창고의 목록은 조회할 수 없다`() = runTest {
            assertThatThrownBy { runBlocking { deniedService().getAllByDisposal(1L).toList() } }
                .isInstanceOf(WarehouseAccessDeniedException::class.java)
            verify(disposalItemRepository, never()).findAllByDisposalId(any())
        }

        @Test
        fun `상위 문서가 없으면 목록 조회는 예외를 던진다`() = runTest {
            whenever(disposalRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { disposalItemService.getAllByDisposal(1L).toList() } }
                .isInstanceOf(DisposalNotFoundException::class.java)
        }
    }
}
