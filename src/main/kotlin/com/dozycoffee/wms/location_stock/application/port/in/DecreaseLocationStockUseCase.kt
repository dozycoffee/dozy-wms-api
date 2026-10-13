package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult

interface DecreaseLocationStockUseCase {
    suspend fun decrease(command: DecreaseLocationStockCommand): LocationStockResult
}
