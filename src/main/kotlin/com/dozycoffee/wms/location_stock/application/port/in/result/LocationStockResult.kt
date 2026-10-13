package com.dozycoffee.wms.location_stock.application.port.`in`.result

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock

data class LocationStockResult(
    val locationStockId: Long,
    val locationId: Long,
    val productId: Long,
    val lotNumber: String,
    val qualityStatus: StockQualityStatus,
    val quantity: Int
) {
    companion object {
        fun from(locationStock: LocationStock): LocationStockResult {
            return LocationStockResult(
                requireNotNull(locationStock.locationStockId),
                locationStock.locationId,
                locationStock.productId,
                locationStock.lotNumber,
                locationStock.qualityStatus,
                locationStock.quantity
            )
        }
    }
}
