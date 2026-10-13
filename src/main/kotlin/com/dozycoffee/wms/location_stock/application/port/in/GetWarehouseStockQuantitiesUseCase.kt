package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.result.WarehouseStockQuantityResult

interface GetWarehouseStockQuantitiesUseCase {
    /**
     * 창고의 상품 x Lot 번호 x 품질 상태별 수량을 돌려준다. [productId], [lotNumber]를 주면 그 조건의 항목만 준다.
     * 수량이 0인 항목은 포함하지 않는다. 없는 창고는 [com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException]이다.
     */
    suspend fun getQuantities(warehouseId: Long, productId: Long?, lotNumber: String?): List<WarehouseStockQuantityResult>
}
