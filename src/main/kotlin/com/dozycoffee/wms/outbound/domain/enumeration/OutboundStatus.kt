package com.dozycoffee.wms.outbound.domain.enumeration

enum class OutboundStatus {
    REQUESTED,
    PICKING,
    INSPECTING,
    COMPLETED;

    fun canTransitionTo(target: OutboundStatus): Boolean = when (this) {
        REQUESTED -> target == PICKING
        PICKING -> target == INSPECTING
        INSPECTING -> target == COMPLETED
        COMPLETED -> false
    }
}
