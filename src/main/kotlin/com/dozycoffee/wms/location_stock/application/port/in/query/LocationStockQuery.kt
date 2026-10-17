package com.dozycoffee.wms.location_stock.application.port.`in`.query

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus

data class LocationStockQuery(
    val warehouseIds: List<Long>? = null,
    val zoneId: Long? = null,
    val locationId: Long? = null,
    val productId: Long? = null,
    val qualityStatus: StockQualityStatus? = null,
    val includeEmpty: Boolean = false
)
