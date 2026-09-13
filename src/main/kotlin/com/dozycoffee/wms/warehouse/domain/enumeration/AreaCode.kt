package com.dozycoffee.wms.warehouse.domain.enumeration

import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity

enum class AreaCode(
    val areaName: String,
    val capacity: Capacity
) {
    INBOUND("입고 처리장", Capacity(50)),
    OUTBOUND("출고장", Capacity(50)),
    RETURN("반품 처리장", Capacity(30)),
    DISPOSAL("폐기 처리장", Capacity(20))
}
