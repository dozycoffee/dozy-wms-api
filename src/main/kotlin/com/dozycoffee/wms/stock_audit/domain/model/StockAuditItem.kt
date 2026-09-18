package com.dozycoffee.wms.stock_audit.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditItemErrorCode

class StockAuditItem private constructor(
    val stockAuditItemId: Long?,
    val stockAuditId: Long,
    val inventoryId: Long,
    val snapshotQuantity: Int,
    countedQuantity: Int?,
    hasUncommittedMovement: Boolean
) : BaseEntity() {

    var countedQuantity: Int? = countedQuantity
        private set

    var hasUncommittedMovement: Boolean = hasUncommittedMovement
        private set

    /** 스냅샷 기준 카운트 차이 — 항상 snapshotQuantity 기준으로 고정한다(ADR-0009) */
    val discrepancy: Int?
        get() = countedQuantity?.let { it - snapshotQuantity }

    val isCounted: Boolean
        get() = countedQuantity != null

    companion object {

        fun create(stockAuditId: Long?, inventoryId: Long?, snapshotQuantity: Int): StockAuditItem {
            val validStockAuditId: Long = requireNonNull(stockAuditId, StockAuditItemErrorCode.INVALID_STOCK_AUDIT_ID)
            val validInventoryId: Long = requireNonNull(inventoryId, StockAuditItemErrorCode.INVALID_INVENTORY_ID)
            validateQuantity(snapshotQuantity, StockAuditItemErrorCode.INVALID_SNAPSHOT_QUANTITY)
            return StockAuditItem(
                stockAuditItemId = null,
                stockAuditId = validStockAuditId,
                inventoryId = validInventoryId,
                snapshotQuantity = snapshotQuantity,
                countedQuantity = null,
                hasUncommittedMovement = false
            )
        }

        fun reconstitute(
            stockAuditItemId: Long,
            stockAuditId: Long,
            inventoryId: Long,
            snapshotQuantity: Int,
            countedQuantity: Int?,
            hasUncommittedMovement: Boolean
        ): StockAuditItem {
            return StockAuditItem(
                stockAuditItemId,
                stockAuditId,
                inventoryId,
                snapshotQuantity,
                countedQuantity,
                hasUncommittedMovement
            )
        }

        private fun validateQuantity(quantity: Int, errorCode: StockAuditItemErrorCode) {
            if (quantity < 0) {
                throw InvalidDomainValueException(errorCode)
            }
        }
    }

    /** 실사자가 실측 수량을 입력한다. 마감 전까지 재입력으로 덮어쓸 수 있다 */
    fun count(countedQuantity: Int) {
        validateQuantity(countedQuantity, StockAuditItemErrorCode.INVALID_COUNTED_QUANTITY)
        this.countedQuantity = countedQuantity
    }

    /** 스냅샷 시각 이후 해당 재고에 정상 입출고 이력이 있었음을 표시한다(단순 오차 판단 힌트, ADR-0009) */
    fun markHasUncommittedMovement() {
        hasUncommittedMovement = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StockAuditItem) return false
        val id: Long? = stockAuditItemId
        return id != null && id == other.stockAuditItemId
    }

    override fun hashCode(): Int {
        return stockAuditItemId?.hashCode() ?: System.identityHashCode(this)
    }
}
