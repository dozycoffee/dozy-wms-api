package com.dozycoffee.wms.inventory.domain.enumeration

enum class AllocationStatus {
    HELD,
    RELEASED,
    FULFILLED;

    fun canTransitionTo(target: AllocationStatus): Boolean = when (this) {
        HELD -> target == RELEASED || target == FULFILLED
        RELEASED -> false
        FULFILLED -> false
    }
}
