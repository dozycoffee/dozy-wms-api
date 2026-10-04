package com.dozycoffee.wms.inventory.domain.enumeration

enum class LotStatus {
    NORMAL,
    EXPIRING_SOON,
    EXPIRED;

    fun canTransitionTo(target: LotStatus): Boolean = when (this) {
        NORMAL -> target == EXPIRING_SOON || target == EXPIRED
        EXPIRING_SOON -> target == EXPIRED
        EXPIRED -> false
    }
}
