package com.dozycoffee.wms.inbound.domain.enumeration

enum class InboundStatus {
    EXPECTED,
    WAITING,
    PROCESSING,
    COMPLETED;

    fun canTransitionTo(target: InboundStatus): Boolean = when (this) {
        EXPECTED -> target == WAITING
        WAITING -> target == PROCESSING
        PROCESSING -> target == COMPLETED
        COMPLETED -> false
    }
}
