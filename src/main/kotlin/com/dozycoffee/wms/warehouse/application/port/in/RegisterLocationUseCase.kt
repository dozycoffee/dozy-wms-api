package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult

interface RegisterLocationUseCase {
    suspend fun register(command: RegisterLocationCommand): LocationResult
}
