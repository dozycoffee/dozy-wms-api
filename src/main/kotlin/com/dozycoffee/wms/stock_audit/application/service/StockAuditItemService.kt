package com.dozycoffee.wms.stock_audit.application.service

import com.dozycoffee.wms.stock_audit.application.port.`in`.CountStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.GetStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.CountStockAuditItemCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditItemResult
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditItemRepository
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditItemNotFoundException
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StockAuditItemService(
    private val stockAuditItemRepository: StockAuditItemRepository
) : CountStockAuditItemUseCase, GetStockAuditItemUseCase {

    @Transactional
    override suspend fun count(command: CountStockAuditItemCommand): StockAuditItemResult {
        val item = findStockAuditItemOrThrow(command.stockAuditItemId)
        item.count(command.countedQuantity)
        return StockAuditItemResult.from(stockAuditItemRepository.save(item))
    }

    @Transactional(readOnly = true)
    override fun getAllByStockAudit(stockAuditId: Long): Flow<StockAuditItemResult> {
        return stockAuditItemRepository.findAllByStockAuditId(stockAuditId).map { StockAuditItemResult.from(it) }
    }

    private suspend fun findStockAuditItemOrThrow(stockAuditItemId: Long): StockAuditItem {
        return stockAuditItemRepository.findById(stockAuditItemId) ?: throw StockAuditItemNotFoundException()
    }
}
