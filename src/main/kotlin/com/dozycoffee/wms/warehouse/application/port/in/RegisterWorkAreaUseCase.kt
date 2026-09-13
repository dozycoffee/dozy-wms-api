package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import reactor.core.publisher.Mono

interface RegisterWorkAreaUseCase {
    fun register(command: RegisterWorkAreaCommand): Mono<WorkAreaResult>
}
