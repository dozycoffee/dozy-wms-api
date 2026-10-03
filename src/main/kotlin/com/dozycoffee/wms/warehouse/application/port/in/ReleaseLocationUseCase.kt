package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult

interface ReleaseLocationUseCase {
    suspend fun release(command: ReleaseLocationCommand): LocationResult
}
