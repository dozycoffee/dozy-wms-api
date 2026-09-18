package com.dozycoffee.wms.stock_audit.fixture

import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem

class StockAuditItemTestBuilder {

    private var stockAuditItemId: Long? = null
    private var stockAuditId: Long? = 1L
    private var inventoryId: Long? = 1L
    private var snapshotQuantity: Int = 10
    private var countedQuantity: Int? = null
    private var hasUncommittedMovement: Boolean = false

    companion object {
        fun stockAuditItem(): StockAuditItemTestBuilder = StockAuditItemTestBuilder()
    }

    fun stockAuditItemId(stockAuditItemId: Long?): StockAuditItemTestBuilder {
        this.stockAuditItemId = stockAuditItemId
        return this
    }

    fun stockAuditId(stockAuditId: Long?): StockAuditItemTestBuilder {
        this.stockAuditId = stockAuditId
        return this
    }

    fun inventoryId(inventoryId: Long?): StockAuditItemTestBuilder {
        this.inventoryId = inventoryId
        return this
    }

    fun snapshotQuantity(snapshotQuantity: Int): StockAuditItemTestBuilder {
        this.snapshotQuantity = snapshotQuantity
        return this
    }

    fun countedQuantity(countedQuantity: Int?): StockAuditItemTestBuilder {
        this.countedQuantity = countedQuantity
        return this
    }

    fun hasUncommittedMovement(hasUncommittedMovement: Boolean): StockAuditItemTestBuilder {
        this.hasUncommittedMovement = hasUncommittedMovement
        return this
    }

    fun build(): StockAuditItem {
        val id: Long? = stockAuditItemId
        if (id != null) {
            return StockAuditItem.reconstitute(
                stockAuditItemId = id,
                stockAuditId = requireNotNull(stockAuditId) { "stockAuditId는 재구성 시 필수입니다." },
                inventoryId = requireNotNull(inventoryId) { "inventoryId는 재구성 시 필수입니다." },
                snapshotQuantity = snapshotQuantity,
                countedQuantity = countedQuantity,
                hasUncommittedMovement = hasUncommittedMovement
            )
        }
        return StockAuditItem.create(
            stockAuditId = stockAuditId,
            inventoryId = inventoryId,
            snapshotQuantity = snapshotQuantity
        )
    }
}
