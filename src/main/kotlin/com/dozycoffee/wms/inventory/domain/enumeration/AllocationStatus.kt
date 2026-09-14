package com.dozycoffee.wms.inventory.domain.enumeration

enum class AllocationStatus(val description: String) {
    HELD("점유중"),
    RELEASED("해제됨"),
    FULFILLED("완료됨");

    fun canTransitionTo(target: AllocationStatus): Boolean = when (this) {
        HELD -> target == RELEASED || target == FULFILLED
        RELEASED -> false
        FULFILLED -> false
    }
}
