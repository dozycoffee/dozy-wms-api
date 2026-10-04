package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult

interface OccupyLocationUseCase {
    suspend fun occupy(command: OccupyLocationCommand): LocationResult
}
