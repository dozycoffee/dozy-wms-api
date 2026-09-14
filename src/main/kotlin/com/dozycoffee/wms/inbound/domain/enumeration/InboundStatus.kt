package com.dozycoffee.wms.inbound.domain.enumeration

enum class InboundStatus(val description: String) {
    EXPECTED("입고 예정"),
    WAITING("입고 대기"),
    PROCESSING("입고 처리중"),
    COMPLETED("입고 완료");

    fun canTransitionTo(target: InboundStatus): Boolean = when (this) {
        EXPECTED -> target == WAITING
        WAITING -> target == PROCESSING
        PROCESSING -> target == COMPLETED
        COMPLETED -> false
    }
}
