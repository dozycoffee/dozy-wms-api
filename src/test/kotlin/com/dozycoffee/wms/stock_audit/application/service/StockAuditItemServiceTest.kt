package com.dozycoffee.wms.stock_audit.application.service

import com.dozycoffee.wms.stock_audit.application.port.`in`.command.CountStockAuditItemCommand
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditItemRepository
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditItemNotFoundException
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import com.dozycoffee.wms.stock_audit.fixture.StockAuditItemTestBuilder.Companion.stockAuditItem
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
class StockAuditItemServiceTest {

    @Mock
    private lateinit var stockAuditItemRepository: StockAuditItemRepository

    @InjectMocks
    private lateinit var stockAuditItemService: StockAuditItemService

    @Nested
    inner class 실측_수량_입력 {

        @Test
        fun `존재하는 항목에 실측 수량을 입력하면 반영된다`() = runTest {
            val found: StockAuditItem = stockAuditItem().stockAuditItemId(1L).snapshotQuantity(20).build()
            whenever(stockAuditItemRepository.findById(1L)).thenReturn(found)
            whenever(stockAuditItemRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = stockAuditItemService.count(CountStockAuditItemCommand(1L, 18))

            assertThat(result.countedQuantity).isEqualTo(18)
            assertThat(result.discrepancy).isEqualTo(-2)
        }

        @Test
        fun `존재하지 않는 항목에 입력하면 예외를 던진다`() = runTest {
            whenever(stockAuditItemRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { stockAuditItemService.count(CountStockAuditItemCommand(1L, 18)) } }
                .isInstanceOf(StockAuditItemNotFoundException::class.java)
        }
    }

    @Nested
    inner class 실사별_항목_목록_조회 {

        @Test
        fun `실사 ID로 조회하면 해당 실사의 항목 목록을 반환한다`() = runTest {
            val item: StockAuditItem = stockAuditItem().stockAuditItemId(1L).stockAuditId(1L).build()
            whenever(stockAuditItemRepository.findAllByStockAuditId(1L)).thenReturn(flowOf(item))

            val result = stockAuditItemService.getAllByStockAudit(1L).toList()

            assertThat(result).hasSize(1)
            assertThat(result[0].stockAuditId).isEqualTo(1L)
        }
    }
}
