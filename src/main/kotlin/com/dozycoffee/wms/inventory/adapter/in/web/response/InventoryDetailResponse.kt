package com.dozycoffee.wms.inventory.adapter.`in`.web.response

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryDetailResult

data class InventoryDetailResponse(
    val inventory: InventoryResponse,
    val lot: LotResponse,
    val recentHistories: List<InventoryHistoryResponse>
) {
    companion object {
        fun from(result: InventoryDetailResult): InventoryDetailResponse {
            return InventoryDetailResponse(
                InventoryResponse.from(result.inventory),
                LotResponse.from(result.lot),
                result.recentHistories.map { InventoryHistoryResponse.from(it) }
            )
        }
    }
}
