package com.dozycoffee.wms.inventory.domain.enumeration

enum class QualityStatus(val description: String) {
    NORMAL("정상"),
    DEFECTIVE("불량"),
    DISPOSAL_SCHEDULED("폐기 예정")
}
