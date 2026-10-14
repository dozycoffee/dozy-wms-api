package com.dozycoffee.wms.outbound.domain.enumeration

enum class OutboundStatus {
    REQUESTED,
    PICKING,
    INSPECTING,
    COMPLETED,
    CANCEL_REQUESTED,
    CANCELED;

    fun canTransitionTo(target: OutboundStatus): Boolean = when (this) {
        REQUESTED -> target == PICKING || target == CANCELED
        PICKING -> target == INSPECTING || target == CANCEL_REQUESTED
        INSPECTING -> target == COMPLETED || target == CANCEL_REQUESTED
        CANCEL_REQUESTED -> target == CANCELED
        COMPLETED, CANCELED -> false
    }
}
