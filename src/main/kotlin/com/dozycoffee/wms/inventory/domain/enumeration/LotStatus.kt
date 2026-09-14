package com.dozycoffee.wms.inventory.domain.enumeration

enum class LotStatus(val description: String) {
    NORMAL("정상"),
    EXPIRING_SOON("유통기한 임박"),
    EXPIRED("유통기한 경과");

    fun canTransitionTo(target: LotStatus): Boolean = when (this) {
        NORMAL -> target == EXPIRING_SOON || target == EXPIRED
        EXPIRING_SOON -> target == EXPIRED
        EXPIRED -> false
    }
}
