package com.dozycoffee.wms.return_request.domain.enumeration

enum class ReturnRequestStatus(val description: String) {
    RECEIVED("반품 접수"),
    INSPECTING("검수 중"),
    COMPLETED("반품 완료");

    fun canTransitionTo(target: ReturnRequestStatus): Boolean = when (this) {
        RECEIVED -> target == INSPECTING
        INSPECTING -> target == COMPLETED
        COMPLETED -> false
    }
}
