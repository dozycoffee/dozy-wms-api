package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditRepository
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class StockAuditPersistenceAdapter(
    private val stockAuditR2dbcRepository: StockAuditR2dbcRepository
) : StockAuditRepository {

    override suspend fun save(stockAudit: StockAudit): StockAudit {
        val entity = StockAuditEntity.from(stockAudit)
        val stockAuditId = stockAudit.stockAuditId
        if (stockAuditId != null) {
            stockAuditR2dbcRepository.findById(stockAuditId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return stockAuditR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(stockAuditId: Long): StockAudit? {
        return stockAuditR2dbcRepository.findById(stockAuditId)?.toDomain()
    }

    override fun findAll(warehouseId: Long?, status: StockAuditStatus?): Flow<StockAudit> {
        val statusCode = status?.let { CommonCodes.toCode(STATUS_GROUP, it) }
        return stockAuditR2dbcRepository.findAllStockAudits(warehouseId, statusCode).map { it.toDomain() }
    }

    companion object {
        private const val STATUS_GROUP = "STOCK_AUDIT_STATUS"
    }
}
