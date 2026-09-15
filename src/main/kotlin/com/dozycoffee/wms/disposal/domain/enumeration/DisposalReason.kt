package com.dozycoffee.wms.disposal.domain.enumeration

enum class DisposalReason(val description: String) {
    EXPIRED("유통기한 경과"),
    INSPECTION_DEFECT("검수 불량"),
    RETURN_DEFECT("반품 불량"),
    OTHER("기타")
}
