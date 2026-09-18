package com.dozycoffee.wms.inventory.application.port.`in`.result

data class InventoryDetailResult(
    val inventory: InventoryResult,
    val lot: LotResult,
    val recentHistories: List<InventoryHistoryResult>
)
