package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import reactor.core.publisher.Mono

interface ReleaseLocationUseCase {
    fun release(command: ReleaseLocationCommand): Mono<LocationResult>
}
