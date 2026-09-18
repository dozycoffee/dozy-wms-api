package com.dozycoffee.wms.inventory.domain.enumeration

enum class InventoryHistoryType(val description: String) {
    INBOUND("입고"),
    OUTBOUND("출고"),
    DISPOSAL("폐기"),
    ADJUSTMENT("조정")
}
