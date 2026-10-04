package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult

interface ReleaseWorkAreaUseCase {
    suspend fun release(command: ReleaseWorkAreaCommand): WorkAreaResult
}
