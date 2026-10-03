package com.dozycoffee.wms.stock_audit.application.service

import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.stock_audit.application.port.`in`.CountStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.GetStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.CountStockAuditItemCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditItemResult
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditItemRepository
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditRepository
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditItemNotFoundException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditNotFoundException
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StockAuditItemService(
    private val stockAuditItemRepository: StockAuditItemRepository,
    private val stockAuditRepository: StockAuditRepository,
    private val warehouseAccessGuard: WarehouseAccessGuard
) : CountStockAuditItemUseCase, GetStockAuditItemUseCase {

    @Transactional
    override suspend fun count(command: CountStockAuditItemCommand): StockAuditItemResult {
        val item = findStockAuditItemOrThrow(command.stockAuditItemId)
        requireParentAccess(item.stockAuditId)
        item.count(command.countedQuantity)
        return StockAuditItemResult.from(stockAuditItemRepository.save(item))
    }

    @Transactional(readOnly = true)
    override fun getAllByStockAudit(stockAuditId: Long): Flow<StockAuditItemResult> {
        return flow {
            requireParentAccess(stockAuditId)
            emitAll(stockAuditItemRepository.findAllByStockAuditId(stockAuditId).map { StockAuditItemResult.from(it) })
        }
    }

    private suspend fun findStockAuditItemOrThrow(stockAuditItemId: Long): StockAuditItem {
        return stockAuditItemRepository.findById(stockAuditItemId) ?: throw StockAuditItemNotFoundException()
    }

    private suspend fun requireParentAccess(stockAuditId: Long) {
        val parent = stockAuditRepository.findById(stockAuditId) ?: throw StockAuditNotFoundException()
        warehouseAccessGuard.require(parent.warehouseId)
    }
}
