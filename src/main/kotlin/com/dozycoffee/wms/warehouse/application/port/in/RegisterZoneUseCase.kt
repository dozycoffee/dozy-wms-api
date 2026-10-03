package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult

interface RegisterZoneUseCase {
    suspend fun register(command: RegisterZoneCommand): ZoneResult
}
