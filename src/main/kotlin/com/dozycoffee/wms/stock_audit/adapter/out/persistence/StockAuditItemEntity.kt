package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("stock_audit_item")
class StockAuditItemEntity private constructor() : BaseEntity() {

    @Id
    var stockAuditItemId: Long? = null
        private set

    var stockAuditId: Long? = null
        private set

    var inventoryId: Long? = null
        private set

    var snapshotQuantity: Int? = null
        private set

    var snapshotTakenAt: LocalDateTime? = null
        private set

    var countedQuantity: Int? = null
        private set

    var hasUncommittedMovement: Boolean = false
        private set

    fun toDomain(): StockAuditItem {
        return StockAuditItem.reconstitute(
            requireNotNull(stockAuditItemId),
            requireNotNull(stockAuditId),
            requireNotNull(inventoryId),
            requireNotNull(snapshotQuantity),
            requireNotNull(snapshotTakenAt),
            countedQuantity,
            hasUncommittedMovement
        )
    }

    companion object {
        fun from(domain: StockAuditItem): StockAuditItemEntity {
            val entity = StockAuditItemEntity()
            entity.stockAuditItemId = domain.stockAuditItemId
            entity.stockAuditId = domain.stockAuditId
            entity.inventoryId = domain.inventoryId
            entity.snapshotQuantity = domain.snapshotQuantity
            entity.snapshotTakenAt = domain.snapshotTakenAt
            entity.countedQuantity = domain.countedQuantity
            entity.hasUncommittedMovement = domain.hasUncommittedMovement
            return entity
        }
    }
}
