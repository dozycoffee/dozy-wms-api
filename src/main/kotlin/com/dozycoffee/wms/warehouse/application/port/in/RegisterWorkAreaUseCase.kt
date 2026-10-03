package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult

interface RegisterWorkAreaUseCase {
    suspend fun register(command: RegisterWorkAreaCommand): WorkAreaResult
}
