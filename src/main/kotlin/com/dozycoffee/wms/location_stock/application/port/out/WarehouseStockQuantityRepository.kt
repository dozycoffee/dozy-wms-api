package com.dozycoffee.wms.location_stock.application.port.out

import com.dozycoffee.wms.location_stock.application.port.`in`.result.WarehouseStockQuantityResult

interface WarehouseStockQuantityRepository {
    suspend fun sumByWarehouse(warehouseId: Long, productId: Long?, lotNumber: String?): List<WarehouseStockQuantityResult>
}
