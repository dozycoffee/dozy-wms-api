package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import reactor.core.publisher.Mono

interface ReleaseWorkAreaUseCase {
    fun release(command: ReleaseWorkAreaCommand): Mono<WorkAreaResult>
}
