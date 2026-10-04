package com.dozycoffee.wms.stock_audit.domain.enumeration

enum class StockAuditStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CLOSED;

    fun canTransitionTo(target: StockAuditStatus): Boolean = when (this) {
        SCHEDULED -> target == IN_PROGRESS
        IN_PROGRESS -> target == COMPLETED
        COMPLETED -> target == CLOSED
        CLOSED -> false
    }
}
