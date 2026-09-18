package com.dozycoffee.wms.stock_audit.fixture

import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit

class StockAuditTestBuilder {

    private var stockAuditId: Long? = null
    private var warehouseId: Long? = 1L
    private var zoneId: Long? = 1L
    private var status: StockAuditStatus = StockAuditStatus.SCHEDULED
    private var assignee: String? = null
    private var approvedBy: String? = null

    companion object {
        fun stockAudit(): StockAuditTestBuilder = StockAuditTestBuilder()
    }

    fun stockAuditId(stockAuditId: Long?): StockAuditTestBuilder {
        this.stockAuditId = stockAuditId
        return this
    }

    fun warehouseId(warehouseId: Long?): StockAuditTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun zoneId(zoneId: Long?): StockAuditTestBuilder {
        this.zoneId = zoneId
        return this
    }

    fun status(status: StockAuditStatus): StockAuditTestBuilder {
        this.status = status
        return this
    }

    fun assignee(assignee: String?): StockAuditTestBuilder {
        this.assignee = assignee
        return this
    }

    fun approvedBy(approvedBy: String?): StockAuditTestBuilder {
        this.approvedBy = approvedBy
        return this
    }

    fun build(): StockAudit {
        val id: Long? = stockAuditId
        if (id != null) {
            return StockAudit.reconstitute(
                stockAuditId = id,
                warehouseId = requireNotNull(warehouseId) { "warehouseId는 재구성 시 필수입니다." },
                zoneId = requireNotNull(zoneId) { "zoneId는 재구성 시 필수입니다." },
                status = status,
                assignee = assignee,
                approvedBy = approvedBy
            )
        }
        return StockAudit.create(warehouseId = warehouseId, zoneId = zoneId)
    }
}
