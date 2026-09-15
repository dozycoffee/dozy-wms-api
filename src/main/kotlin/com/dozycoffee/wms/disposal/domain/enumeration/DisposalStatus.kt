package com.dozycoffee.wms.disposal.domain.enumeration

enum class DisposalStatus(val description: String) {
    REQUESTED("폐기 요청"),
    APPROVED("폐기 승인"),
    COMPLETED("폐기 완료");

    fun canTransitionTo(target: DisposalStatus): Boolean = when (this) {
        REQUESTED -> target == APPROVED
        APPROVED -> target == COMPLETED
        COMPLETED -> false
    }
}
