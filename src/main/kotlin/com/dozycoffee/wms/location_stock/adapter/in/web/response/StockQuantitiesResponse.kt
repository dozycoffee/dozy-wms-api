package com.dozycoffee.wms.location_stock.adapter.`in`.web.response

import com.dozycoffee.wms.location_stock.application.port.`in`.result.WarehouseStockQuantityResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus

/** inventory 서비스 대사가 읽는 계약이다. 필드 이름을 바꾸지 않는다(inventory ADR-0033) */
data class StockQuantitiesResponse(
    val items: List<Item>
) {
    data class Item(
        val productId: Long,
        val lotNumber: String,
        val qualityStatus: StockQualityStatus,
        val quantity: Int
    )

    companion object {
        fun from(results: List<WarehouseStockQuantityResult>): StockQuantitiesResponse {
            return StockQuantitiesResponse(
                results.map { Item(it.productId, it.lotNumber, it.qualityStatus, it.quantity) }
            )
        }
    }
}
