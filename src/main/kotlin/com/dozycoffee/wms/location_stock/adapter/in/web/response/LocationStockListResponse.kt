package com.dozycoffee.wms.location_stock.adapter.`in`.web.response

import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockDetailResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import java.time.LocalDateTime

data class LocationStockListResponse(
    val items: List<LocationStockItemResponse>
) {
    companion object {
        fun from(results: List<LocationStockDetailResult>): LocationStockListResponse {
            return LocationStockListResponse(results.map { LocationStockItemResponse.from(it) })
        }
    }
}

data class LocationStockItemResponse(
    val locationStockId: Long,
    val warehouseId: Long,
    val zoneId: Long,
    val zoneCode: ZoneCode,
    val locationId: Long,
    val locationCode: String,
    val productId: Long,
    val lotNumber: String,
    val qualityStatus: StockQualityStatus,
    val quantity: Int,
    val heldQuantity: Int,
    val availableQuantity: Int,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(result: LocationStockDetailResult): LocationStockItemResponse {
            return LocationStockItemResponse(
                result.locationStockId,
                result.warehouseId,
                result.zoneId,
                result.zoneCode,
                result.locationId,
                result.locationCode,
                result.productId,
                result.lotNumber,
                result.qualityStatus,
                result.quantity,
                result.heldQuantity,
                result.availableQuantity,
                result.updatedAt
            )
        }
    }
}
