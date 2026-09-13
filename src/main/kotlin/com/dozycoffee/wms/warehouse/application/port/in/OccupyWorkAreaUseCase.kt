package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import reactor.core.publisher.Mono

interface OccupyWorkAreaUseCase {
    fun occupy(command: OccupyWorkAreaCommand): Mono<WorkAreaResult>
}
