package com.dozycoffee.wms.location_stock.application.port.`in`.result

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus

/** 창고 안의 모든 Location을 합친 상품 x Lot 번호 x 품질 상태별 수량 */
data class WarehouseStockQuantityResult(
    val productId: Long,
    val lotNumber: String,
    val qualityStatus: StockQualityStatus,
    val quantity: Int
)
