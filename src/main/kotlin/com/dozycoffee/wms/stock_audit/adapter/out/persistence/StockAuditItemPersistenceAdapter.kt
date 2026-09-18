package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditItemRepository
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class StockAuditItemPersistenceAdapter(
    private val stockAuditItemR2dbcRepository: StockAuditItemR2dbcRepository
) : StockAuditItemRepository {

    override suspend fun save(stockAuditItem: StockAuditItem): StockAuditItem {
        val entity = StockAuditItemEntity.from(stockAuditItem)
        val stockAuditItemId = stockAuditItem.stockAuditItemId
        if (stockAuditItemId != null) {
            stockAuditItemR2dbcRepository.findById(stockAuditItemId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return stockAuditItemR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(stockAuditItemId: Long): StockAuditItem? {
        return stockAuditItemR2dbcRepository.findById(stockAuditItemId)?.toDomain()
    }

    override fun findAllByStockAuditId(stockAuditId: Long): Flow<StockAuditItem> {
        return stockAuditItemR2dbcRepository.findAllByStockAuditId(stockAuditId).map { it.toDomain() }
    }
}
