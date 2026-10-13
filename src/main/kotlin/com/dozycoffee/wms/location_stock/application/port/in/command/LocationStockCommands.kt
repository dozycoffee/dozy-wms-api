package com.dozycoffee.wms.location_stock.application.port.`in`.command

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus

data class IncreaseLocationStockCommand(
    val locationId: Long,
    val productId: Long,
    val lotNumber: String,
    val qualityStatus: StockQualityStatus,
    val quantity: Int
)

data class DecreaseLocationStockCommand(
    val locationId: Long,
    val productId: Long,
    val lotNumber: String,
    val qualityStatus: StockQualityStatus,
    val quantity: Int
)

data class TransferLocationStockQualityCommand(
    val locationId: Long,
    val productId: Long,
    val lotNumber: String,
    val fromQualityStatus: StockQualityStatus,
    val toQualityStatus: StockQualityStatus,
    val quantity: Int
)
