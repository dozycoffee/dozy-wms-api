package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult

interface OccupyWorkAreaUseCase {
    suspend fun occupy(command: OccupyWorkAreaCommand): WorkAreaResult
}
