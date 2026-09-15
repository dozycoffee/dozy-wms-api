package com.dozycoffee.wms.return_request.domain.enumeration

enum class ReturnInspectionResult(val description: String) {
    PENDING("검수 대기"),
    NORMAL("정상"),
    DEFECTIVE("불량")
}
