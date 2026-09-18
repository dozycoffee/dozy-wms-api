package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("stock_audit")
class StockAuditEntity private constructor() : BaseEntity() {

    @Id
    var stockAuditId: Long? = null
        private set

    var warehouseId: Long? = null
        private set

    var zoneId: Long? = null
        private set

    var status: String? = null
        private set

    var assignee: String? = null
        private set

    var approvedBy: String? = null
        private set

    fun toDomain(): StockAudit {
        return StockAudit.reconstitute(
            requireNotNull(stockAuditId),
            requireNotNull(warehouseId),
            requireNotNull(zoneId),
            CommonCodes.fromCode(StockAuditStatus::class.java, requireNotNull(status)),
            assignee,
            approvedBy
        )
    }

    companion object {
        private const val STATUS_GROUP = "STOCK_AUDIT_STATUS"

        fun from(domain: StockAudit): StockAuditEntity {
            val entity = StockAuditEntity()
            entity.stockAuditId = domain.stockAuditId
            entity.warehouseId = domain.warehouseId
            entity.zoneId = domain.zoneId
            entity.status = CommonCodes.toCode(STATUS_GROUP, domain.status)
            entity.assignee = domain.assignee
            entity.approvedBy = domain.approvedBy
            return entity
        }
    }
}
