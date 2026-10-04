package com.dozycoffee.wms.return_request.domain.enumeration

enum class ReturnRequestStatus {
    RECEIVED,
    INSPECTING,
    COMPLETED;

    fun canTransitionTo(target: ReturnRequestStatus): Boolean = when (this) {
        RECEIVED -> target == INSPECTING
        INSPECTING -> target == COMPLETED
        COMPLETED -> false
    }
}
