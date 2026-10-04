package com.dozycoffee.wms.disposal.domain.enumeration

enum class DisposalStatus {
    REQUESTED,
    APPROVED,
    COMPLETED;

    fun canTransitionTo(target: DisposalStatus): Boolean = when (this) {
        REQUESTED -> target == APPROVED
        APPROVED -> target == COMPLETED
        COMPLETED -> false
    }
}
