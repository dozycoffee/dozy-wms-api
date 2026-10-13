package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult

interface IncreaseLocationStockUseCase {
    suspend fun increase(command: IncreaseLocationStockCommand): LocationStockResult
}
