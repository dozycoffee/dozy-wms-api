package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.query.LocationStockQuery
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockDetailResult
import kotlinx.coroutines.flow.Flow

interface ListLocationStocksUseCase {
    fun getAll(query: LocationStockQuery): Flow<LocationStockDetailResult>
}
