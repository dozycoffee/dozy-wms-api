package com.dozycoffee.wms.stock_audit.domain.enumeration

enum class StockAuditStatus(val description: String) {
    SCHEDULED("실사 예정"),
    IN_PROGRESS("실사 진행중"),
    COMPLETED("실사 완료"),
    CLOSED("조정 마감");

    fun canTransitionTo(target: StockAuditStatus): Boolean = when (this) {
        SCHEDULED -> target == IN_PROGRESS
        IN_PROGRESS -> target == COMPLETED
        COMPLETED -> target == CLOSED
        CLOSED -> false
    }
}
