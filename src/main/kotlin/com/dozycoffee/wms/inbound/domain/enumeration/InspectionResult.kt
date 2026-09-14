package com.dozycoffee.wms.inbound.domain.enumeration

enum class InspectionResult(val description: String) {
    PENDING("검수 대기"),
    NORMAL("정상"),
    DEFECTIVE("불량")
}
