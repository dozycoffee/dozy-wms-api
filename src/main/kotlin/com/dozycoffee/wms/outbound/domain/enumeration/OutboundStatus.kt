package com.dozycoffee.wms.outbound.domain.enumeration

enum class OutboundStatus(val description: String) {
    REQUESTED("출고 요청"),
    PICKING("피킹중"),
    INSPECTING("검수중"),
    COMPLETED("출고 완료");

    fun canTransitionTo(target: OutboundStatus): Boolean = when (this) {
        REQUESTED -> target == PICKING
        PICKING -> target == INSPECTING
        INSPECTING -> target == COMPLETED
        COMPLETED -> false
    }
}
