package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.result.ZoneStockSummaryResult
import kotlinx.coroutines.flow.Flow

interface GetZoneStockSummaryUseCase {
    fun getAll(warehouseIds: List<Long>?): Flow<ZoneStockSummaryResult>
}
